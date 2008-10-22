package mcast.ht.bittorrent;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

import mcast.ht.admin.PieceIndexSet;
import mcast.ht.admin.PieceIndexSetFactory;
import mcast.ht.storage.Piece;
import mcast.ht.storage.Storage;
import mcast.ht.util.Convert;
import mcast.ht.util.ManagementProperties;

import org.apache.log4j.Logger;

/**
 * @author mathijs
 */
public class BitTorrentCommunicator implements Config, MessageUpcall {

    private Logger logger = Logger.getLogger(BitTorrentCommunicator.class);

    private static final byte OPCODE_CHOKE = 0;
    private static final byte OPCODE_UNCHOKE = 1;
    private static final byte OPCODE_INTERESTED = 2;
    private static final byte OPCODE_NOT_INTERESTED = 3;
    private static final byte OPCODE_HAVE = 4;
    private static final byte OPCODE_BITFIELD = 5;
    private static final byte OPCODE_REQUEST = 6;
    private static final byte OPCODE_PIECE = 7;
    private static final byte OPCODE_CANCEL = 8;
    private static final byte OPCODE_DONE = 9;
    private static final byte OPCODE_STOP = 10;

    private final IbisIdentifier me, peer;
    private final BitTorrentUpcall upcall;
    private volatile Storage storage;
    private volatile SendPort sport;
    private volatile ReceivePort rport;
    private final String upcallName;
    private volatile RateEstimate uploadRateEstimate;
    private volatile RateEstimate downloadRateEstimate;
    private volatile boolean sendingStopped;
    private final Object sendingStoppedLock = new Object();
    private volatile boolean receivingStopped;

    public BitTorrentCommunicator(IbisIdentifier me, IbisIdentifier peer, 
            BitTorrentUpcall upcall, boolean estimateDownloadRate, 
            boolean estimateUploadRate) {
        this.me = me;
        this.peer = peer;
        this.upcall = upcall;
        this.storage = null;

        sport = null;
        rport = null;

        if (estimateDownloadRate) {
            downloadRateEstimate = new RateEstimate(RATE_ESTIMATE_PERIOD);
        }
        if (estimateUploadRate) {
            uploadRateEstimate = new RateEstimate(RATE_ESTIMATE_PERIOD);
        }

        upcallName = "R " + peer + " > " + me;

        sendingStopped = true;
        receivingStopped = true;
    }

    void setSendPort(SendPort sport) {
        this.sport = sport;
    }

    void setReceivePort(ReceivePort rport) {
        this.rport = rport;
    }

    void start(Storage storage) {
        if (storage == null) {
            throw new NullPointerException("storage cannot be null");
        }

        logger.debug("start listening to " + peer);

        // TODO
        //		while (rport.connectedTo().length < 1) {
        //			logger.info("peer " + peer
        //					+ " did not connect yet, sleeping 1 sec...");
        //			Threads.doSleep(1000);
        //		}

        this.storage = storage;

        sendingStopped = false;
        receivingStopped = false;

        rport.enableMessageUpcalls();
    }

    public void waitUntilSendingStopped() {
        synchronized (sendingStoppedLock) {
            while (!sendingStopped) {
                logger.debug("waiting for me to stop sending to " + peer
                        + "...");

                try {
                    sendingStoppedLock.wait();
                } catch (InterruptedException e) {
                    logger.fatal("Interrupted while waiting for me to stop " +
                            "sending to " + peer);
                }
            }
        }
        logger.debug("we stopped sending to " + peer);
    }

    private void stopSending() {
        if (logger.isInfoEnabled()) {
            logger.info("stop sending to " + peer);
        }

        synchronized (sendingStoppedLock) {
            sendingStopped = true;
            sendingStoppedLock.notifyAll();
        }

        if (receivingStopped) {
            storage = null;
        }
    }

    private void stopListening() {
        if (logger.isInfoEnabled()) {
            logger.info("stop listening to " + peer);
        }

        rport.disableMessageUpcalls();
        receivingStopped = true;

        if (sendingStopped) {
            storage = null;
        }
    }

    void sendChoke() throws IOException {
        sendMessage(OPCODE_CHOKE);
    }

    void sendUnchoke() throws IOException {
        sendMessage(OPCODE_UNCHOKE);
    }

    void sendInterested() throws IOException {
        sendMessage(OPCODE_INTERESTED);
    }

    void sendNotInterested() throws IOException {
        sendMessage(OPCODE_NOT_INTERESTED);
    }

    void sendCancel(int pieceIndex) throws IOException {
        sendMessage(OPCODE_CANCEL, pieceIndex);
    }

    void sendDone() throws IOException {
        sendMessage(OPCODE_DONE);
    }

    void sendStop() throws IOException {
        sendMessage(OPCODE_STOP);

        stopSending();
    }

    void sendHave(int pieceIndex) throws IOException {
        sendMessage(OPCODE_HAVE, pieceIndex);
    }

    void sendRequest(int pieceIndex) throws IOException {
        sendMessage(OPCODE_REQUEST, pieceIndex);
    }

    protected String messageName(int opcode) {
        switch (opcode) {
        case OPCODE_CHOKE:
            return "choke";
        case OPCODE_UNCHOKE:
            return "unchoke";
        case OPCODE_INTERESTED:
            return "interested";
        case OPCODE_NOT_INTERESTED:
            return "not interested";
        case OPCODE_HAVE:
            return "have";
        case OPCODE_BITFIELD:
            return "bitfield";
        case OPCODE_REQUEST:
            return "request";
        case OPCODE_PIECE:
            return "piece";
        case OPCODE_CANCEL:
            return "cancel";
        case OPCODE_DONE:
            return "done";
        case OPCODE_STOP:
            return "stop";
        default:
            return "unknown (" + opcode + ")";
        }
    }

    protected void sendMessage(byte opcode) throws IOException {
        if (sendingStopped) {
            logger.warn("not sending " + messageName(opcode)
                    + " because we stopped");
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("S " + messageName(opcode));
        }

        WriteMessage msg = sport.newMessage();
        msg.writeByte(opcode);
        long bytesSent = msg.finish();

        if (uploadRateEstimate != null) {
            uploadRateEstimate.updateRate(bytesSent);
        }
    }

    protected void sendMessage(byte opcode, int payload) throws IOException {
        if (sendingStopped) {
            if (logger.isDebugEnabled()) {
                logger.debug("not sending " + messageName(opcode) + "(" + payload
                        + ") because we stopped");
            }
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("S " + messageName(opcode) + ' ' + payload);
        }

        WriteMessage msg = sport.newMessage();

        msg.writeByte(opcode);
        msg.writeInt(payload);
        long bytesSent = msg.finish();

        if (uploadRateEstimate != null) {
            uploadRateEstimate.updateRate(bytesSent);
        }
    }

    void sendBitfield(PieceIndexSet pieceIndices) throws IOException {
        sendPieceIndexSetMessage(OPCODE_BITFIELD, pieceIndices);
    }

    protected void sendPieceIndexSetMessage(byte opcode,
            PieceIndexSet pieceIndices) throws IOException {
        if (sendingStopped) {
            if (logger.isDebugEnabled()) {
                logger.debug("not sending " + messageName(opcode)
                        + " because we stopped");
            }
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("S " + messageName(opcode) + ' ' + pieceIndices);
        }

        WriteMessage msg = sport.newMessage();

        msg.writeByte(opcode);
        pieceIndices.writeTo(msg);
        long bytesSent = msg.finish();

        if (uploadRateEstimate != null) {
            uploadRateEstimate.updateRate(bytesSent);
        }
            }

    void sendPiece(Piece piece) throws IOException {
        if (sendingStopped) {
            logger.warn("not sending piece " + piece + " because we stopped");
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("S piece " + piece.getIndex());
        }

        WriteMessage msg = sport.newMessage();

        msg.writeByte(OPCODE_PIECE);
        storage.writePiece(piece, msg);
        long bytesSent = msg.finish();

        if (uploadRateEstimate != null) {
            uploadRateEstimate.updateRate(bytesSent);
        }
    }

    public void upcall(ReadMessage m) throws IOException {
        Thread.currentThread().setName(upcallName);

        try {
            byte opcode = m.readByte();

            handleMessage(opcode, m);

        } catch (Throwable e) {
            logger.error("caught throwable in upcall", e);
        } finally {
            if (downloadRateEstimate != null) {
                downloadRateEstimate.updateRate(m.bytesRead());
            }
        }
    }

    protected void handleMessage(byte opcode, ReadMessage m) 
            throws IOException {
        switch (opcode) {
        case OPCODE_CHOKE:
            logger.trace("R choke");
            upcall.receiveChoke();
            break;
        case OPCODE_UNCHOKE:
            logger.trace("R unchoke");
            upcall.receiveUnchoke();
            break;
        case OPCODE_INTERESTED:
            logger.trace("R interested");
            upcall.receiveInterested();
            break;
        case OPCODE_NOT_INTERESTED:
            logger.trace("R not interested");
            upcall.receiveNotInterested();
            break;
        case OPCODE_HAVE: {
            int pieceIndex = m.readInt();
            if (logger.isTraceEnabled()) {
                logger.trace("R have " + pieceIndex);
            }
            upcall.receiveHave(pieceIndex);
            break;
        }
        case OPCODE_BITFIELD: {
            PieceIndexSet pieceIndices = 
                PieceIndexSetFactory.readPieceIndexSet(m);
            if (logger.isTraceEnabled()) {
                logger.trace("R bitfield " + pieceIndices);
            }
            upcall.receiveBitfield(pieceIndices);
            break;
        }
        case OPCODE_REQUEST: {
            int pieceIndex = m.readInt();
            if (logger.isTraceEnabled()) {
                logger.trace("R request " + pieceIndex);
            }
            upcall.receiveRequest(pieceIndex);
            break;
        }
        case OPCODE_PIECE: {
            Piece piece = storage.readPiece(m);
            if (logger.isTraceEnabled()) {
                logger.trace("R piece " + piece.getIndex());
            }
            upcall.receivePiece(piece);
            break;
        }
        case OPCODE_CANCEL: {
            int pieceIndex = m.readInt();
            if (logger.isTraceEnabled()) {
                logger.trace("R cancel " + pieceIndex);
            }
            upcall.receiveCancel(pieceIndex);
            break;
        }
        case OPCODE_DONE: {
            logger.trace("R done");
            upcall.receiveDone();
            break;
        }
        case OPCODE_STOP: {
            logger.trace("R stop");
            stopListening();
            upcall.receiveStop();
            break;
        }
        default:
            logger.error("unknown opcode: " + opcode);
        }
    }

    void close() throws IOException {
        logger.debug("closing send port to " + peer + "...");
        if (sport != null)
            sport.close();
        logger.debug("closing receive port from " + peer + "...");
        if (rport != null)
            rport.close();
        logger.debug("connection to " + peer + " closed");
    }

    double getDownloadBytesPerNanosec() {
        if (downloadRateEstimate != null) {
            return downloadRateEstimate.getRatePerNanosec();
        } else {
            throw new RuntimeException("no rate maintainance requested");
        }
    }

    double getUploadBytesPerNanosec() {
        if (uploadRateEstimate != null) {
            return uploadRateEstimate.getRatePerNanosec();
        } else {
            throw new RuntimeException("no rate maintainance requested");
        }
    }

    void printStats() {
        long bytesSent = ManagementProperties.getMessageBytes(sport);
        long bytesReceived = ManagementProperties.getMessageBytes(rport);

        Config.statsLogger.info(me + " comm_stats " + peer + ": " + "sent " + 
                Convert.bytesToMBytes(bytesSent) + " MB, received " +
                Convert.bytesToMBytes(bytesReceived) + " MB");

        ManagementProperties.setMessageBytes(sport, 0);
        ManagementProperties.setMessageBytes(rport, 0);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            BitTorrentCommunicator other = (BitTorrentCommunicator) o;
            return peer.equals(other.peer);
        }
    }

    public String toString() {
        return "connection " + me + "<->" + peer;
    }

}
