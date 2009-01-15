package mcast.ht.bittorrent;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.util.ThreadPool;
import ibis.util.Timer;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import mcast.ht.admin.P2PAdmin;
import mcast.ht.admin.PieceIndexSet;
import mcast.ht.net.P2PConnection;
import mcast.ht.storage.Piece;
import mcast.ht.storage.Storage;
import mcast.ht.util.Convert;

import org.apache.log4j.Logger;

public class BitTorrentConnection 
implements Config, BitTorrentUpcall, P2PConnection {

    private static Logger logger = Logger.getLogger(BitTorrentConnection.class);

    private static final PortType portType = new PortType(
            PortType.CONNECTION_ONE_TO_ONE, 
            PortType.COMMUNICATION_FIFO,
            PortType.COMMUNICATION_RELIABLE,
            PortType.RECEIVE_AUTO_UPCALLS,
            PortType.SERIALIZATION_DATA);
    
    protected final String poolName;
    protected final IbisIdentifier me, peer;
    protected final BitTorrentAsyncSender asyncSender;

    private final boolean choking;
    private final BitTorrentCommunicator communicator;
    private final Timer piecePickTimer;
    private final List<InterestListener> interestListeners;

    protected volatile BitTorrentAdmin admin;
    protected volatile Storage storage;

    private volatile boolean amChoked;
    private volatile boolean amInterested;
    private volatile boolean peerChoked;
    private volatile boolean peerInterested;
    private volatile boolean peerHasPieces;
    private volatile boolean meDone;
    protected volatile boolean peerDone;
    private volatile boolean meStopped;
    private volatile boolean peerStopped;
    private final Object peerStopLock = new Object();

    private volatile int piecesSent;
    private volatile int piecesReceived;
    private volatile int pendingRequests;
    private volatile int maxPendingRequests;

    public BitTorrentConnection(String poolName, IbisIdentifier me, 
            IbisIdentifier peer, boolean choking, boolean estimateDownloadRate, 
            boolean estimateUploadRate) {
        this.poolName = poolName;
        this.me = me;
        this.peer = peer;
        this.choking = choking;

        admin = null;
        storage = null;

        maxPendingRequests = MAX_PENDING_REQUESTS;

        piecePickTimer = Timer.createTimer();

        resetChoking();

        meDone = true;
        peerDone = true;
        meStopped = true;
        peerStopped = true;

        interestListeners = new LinkedList<InterestListener>();

        communicator = createCommunicator(me, peer, estimateDownloadRate,
                estimateUploadRate);

        asyncSender = createAsyncSender();

        ThreadPool.createNew(asyncSender, "S " + me + " > " + peer);
    }

    public static PortType getPortType() {
        return portType;
    }
    
    protected BitTorrentCommunicator createCommunicator(IbisIdentifier me, 
            IbisIdentifier peer, boolean estimateDownloadRate, 
            boolean estimateUploadRate) {
        return new BitTorrentCommunicator(me, peer, this, estimateDownloadRate,
                estimateUploadRate);
    }

    protected BitTorrentAsyncSender createAsyncSender() {
        return new BitTorrentAsyncSender(communicator);
    }

    void addInterestListener(InterestListener l) {
        synchronized (interestListeners) {
            interestListeners.add(l);
        }
    }

    void removeInterestListener(InterestListener l) {
        synchronized (interestListeners) {
            interestListeners.remove(l);
        }
    }

    private void resetChoking() {
        amChoked = true;
        amInterested = false;
        peerChoked = true;
        peerInterested = false;
        peerHasPieces = false;
    }

    private String createRportName(IbisIdentifier src, IbisIdentifier dst) {
        return poolName + "-rx-" + dst + "->" + src;
    }
    
    private String createSportName(IbisIdentifier src, IbisIdentifier dst) {
        return poolName + "-tx-" + src + "->" + dst;
    }

    public void enableConnect(Ibis ibis) throws IOException {
        logger.info("enabling connections from " + peer);

        String rportName = createRportName(me, peer);
        
        logger.debug("creating receive port " + rportName);
        ReceivePort rport = ibis.createReceivePort(portType, rportName, 
                communicator);
        
        rport.enableConnections();
        communicator.setReceivePort(rport);
    }

    public void connect(Ibis ibis) throws IOException {
        logger.info("connecting to " + peer);

        String sportName = createSportName(me, peer);
        SendPort sport = ibis.createSendPort(portType, sportName);
        String peerRportName = createRportName(peer, me);
		
        logger.debug("connecting sendport " + sportName + 
		        " to receiveport " + peerRportName);
		sport.connect(peer, peerRportName);

		communicator.setSendPort(sport);
    }

    public synchronized void init(Storage storage, P2PAdmin admin) {
        if (logger.isDebugEnabled()) {
            logger.debug("initializing connection to " + peer);
        }

        this.storage = storage;
        this.admin = (BitTorrentAdmin)admin;

        piecesSent = 0;
        piecesReceived = 0;

        resetChoking();

        meDone = false;
        peerDone = false;
        meStopped = false;
        peerStopped = false;

        piecePickTimer.reset();
        
        communicator.init(storage);
    }

    public void start() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("starting connection to " + peer);
        }

        if (storage == null || admin == null) {
            throw new IllegalStateException("connection to " + peer
                    + " has not been initialized");
        }

        // start listening to our peer
        communicator.start();

        startCommunication();

        // check if we are already done (as is the case with seed nodes)
        checkMeDone();
    }

    /**
     * Start communication: inform our peer about the pieces we have
     */
    protected void startCommunication() {
        PieceIndexSet piecesAvailable = getPiecesAvailableForPeer(admin);

        if (!piecesAvailable.isEmpty()) {
            asyncSender.enqueueBitfield(piecesAvailable);
        }
    }

    protected PieceIndexSet getPiecesAvailableForPeer(BitTorrentAdmin admin) {
        return admin.getPiecesReceived();
    }

    public void cancelPiece(int pieceIndex) {
        asyncSender.enqueueCancel(pieceIndex);
    } 

    public void stop() {
        // first, wait until we stopped
        if (logger.isDebugEnabled()) {
            logger.debug("waiting for me to stop sending to " + peer + "...");
        }
        communicator.waitUntilSendingStopped();

        // second, wait for our peer to stop

        synchronized (peerStopLock) {
            while (!peerStopped) {
                if (logger.isDebugEnabled()) {
                    logger.debug("we stopped, waiting for peer " + peer
                            + " to stop sending to us...");
                }

                try {
                    peerStopLock.wait();
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting for peer " + peer
                            + " to stop sending to us");
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("peer " + peer + " stopped sending to us");
        }

        resetChoking();
    }

    public Object getPeer() {
        return peer;
    }

    public double getDownloadBytesPerNanosec() {
        return communicator.getDownloadBytesPerNanosec();
    }

    public double getUploadBytesPerNanosec() {
        return communicator.getUploadBytesPerNanosec();
    }

    boolean peerChoked() {
        return peerChoked;
    }

    boolean peerInterested() {
        return peerInterested;
    }

    boolean amChoked() {
        return amChoked;
    }

    boolean amInterested() {
        return amInterested;
    }

    boolean peerHasPieces() {
        return peerHasPieces;
    }

    void choke() {
        if (logger.isDebugEnabled()) {
            logger.debug("choking " + peer);
        }
        peerChoked = true;
        asyncSender.enqueueChoke();
    }

    void unchoke() {
        if (logger.isDebugEnabled()) {
            logger.debug("unchoking " + peer);
        }
        peerChoked = false;
        asyncSender.enqueueUnchoke();
    }

    public void interested() {
        if (!amInterested) {

            amInterested = true;

            if (logger.isDebugEnabled()) {
                logger.debug("I'm interested in " + peer);
            }

            asyncSender.enqueueInterested();
        }
    }

    public void notInterested() {
        if (amInterested) {

            amInterested = false;

            if (logger.isDebugEnabled()) {
                logger.debug("I'm not interested in " + peer + " anymore");
            }

            asyncSender.enqueueNotInterested();
        }
    }

    protected void requestMorePieces() {
        if (!choking || !amChoked) {
            int[] pieceIndices = null;
            int backlog;

            synchronized (this) {
                backlog = maxPendingRequests - pendingRequests;

                if (backlog > 0) {
                    piecePickTimer.start();
                    pieceIndices = admin.requestDesiredPieceIndices(peer, backlog);
                    piecePickTimer.stop();

                    pendingRequests += pieceIndices.length;
                }
            }

            if (backlog > 0) {
                if (pieceIndices.length > 0) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("requesting " + pieceIndices.length
                                + " more pieces from " + peer + ": "
                                + Arrays.toString(pieceIndices));
                    }
                    asyncSender.enqueueRequest(pieceIndices);
                } else {
                    // there was room to ask more pieces, but we did not;
                    // apparently, we desire no more pieces from our peer
                    if (logger.isDebugEnabled()) {
                        logger.debug("we desire no more pieces from " + peer);
                    }
                    notInterested();
                }
            }
        }
    }

    public void receiveChoke() {
        if (logger.isDebugEnabled()) {
            logger.debug(peer + " choked me");
        }

        amChoked = true;
    }

    public void receiveUnchoke() {
        if (logger.isDebugEnabled()) {
            logger.debug(peer + " unchoked me");
        }

        amChoked = false;
        requestMorePieces();
    }

    public void receiveInterested() {
        if (logger.isDebugEnabled()) {
            logger.debug(peer + " is interested");
        }

        peerInterested = true;

        synchronized (interestListeners) {
            for (InterestListener l : interestListeners) {
                l.interested(this);
            }
        }
    }

    public void receiveNotInterested() {
        if (logger.isDebugEnabled()) {
            logger.debug(peer + " is not interested anymore");
        }

        peerInterested = false;

        synchronized (interestListeners) {
            for (InterestListener l : interestListeners) {
                l.notInterested(this);
            }
        }
    }

    public void receiveHave(int pieceIndex) {
        if (logger.isDebugEnabled()) {
            logger.debug(peer + " has piece " + pieceIndex);
        }

        peerHasPieces = true;

        if (admin.addExistence(peer, pieceIndex)) {
            // peer has a piece we do not have; we are interested!
            interested();
            requestMorePieces();
        }
    }

    public void receiveBitfield(PieceIndexSet pieceIndices) {
        if (logger.isDebugEnabled()) {
            logger.debug(peer + " has pieces " + pieceIndices);
        }

        if (!pieceIndices.isEmpty()) {
            peerHasPieces = true;

            if (admin.addExistence(peer, pieceIndices)) {
                // peer has one or more pieces we do not have, so we are
                // interested
                interested();
                requestMorePieces();
            }
        }
    }

    public void receiveRequest(int pieceIndex) {
        if (logger.isDebugEnabled()) {
            logger.debug(peer + " requested " + pieceIndex);
        }

        if (admin.isPieceReceived(pieceIndex)) {
            try {
                Piece piece = storage.createPiece(pieceIndex);
                sendPieceToPeer(piece);
            } catch (IOException e) {
                logger.fatal("Could not create piece " + pieceIndex);
                logger.fatal(e);
            }
        } else {
            logger.error("we do not have piece " + pieceIndex + " for " + peer);
        }
    }

    protected void sendPieceToPeer(Piece piece) {
        // send the piece
        asyncSender.enqueuePiece(piece);

        piecesSent++;

        // keep track of the fact that peer will now (in the nearby future) 
        // have this piece, since it will not inform us of the existence 
        // with a have messsage
        admin.addExistence(peer, piece.getIndex());
    }

    public void receivePiece(Piece piece) {
        if (logger.isDebugEnabled()) {
            logger.debug("received piece " + piece + " from " + peer);
        }

        admin.setPieceReceived(peer, piece);

        piecesReceived++;
        pendingRequests--;

        if (!meDone) {
            requestMorePieces();
        }
    }

    public void receiveCancel(int pieceIndex) {
        if (logger.isDebugEnabled()) {
            logger.debug("received cancel " + pieceIndex + " from " + peer);
        }

        asyncSender.cancel(pieceIndex);
    }

    public void receiveDone() {
        if (logger.isDebugEnabled()) {
            logger.debug("received done from " + peer);
        }

        peerDone = true;

        checkMeDone();

        checkMeStop();
    }

    protected synchronized boolean checkMeStop() {
        if (!meDone) {
            logger.debug("we do not stop because we are not done");
            return false;
        }

        if (!peerDone) {
            logger
            .debug("we do not stop because peer " + peer + " is not done");
            return false;
        }

        if (!meStopped) {
            logger.debug("telling peer " + peer + " we stop");

            asyncSender.enqueueStop();

            meStopped = true;
        }

        return true;
    }

    public void receiveStop() {
        if (logger.isDebugEnabled()) {
            logger.debug("received stop from " + peer);
        }

        synchronized (peerStopLock) {
            peerStopped = true;
            peerStopLock.notifyAll();
        }
    }

    protected void checkMeDone() {
        if (admin.areAllPieceReceived()) {
            sendMeDone();
        }
    }

    protected synchronized void sendMeDone() {
        if (!meDone) {
            logger.debug("telling peer " + peer + " I am done");
            asyncSender.enqueueDone();
        }

        meDone = true;
    }

    public void setMeDone() {
        // not used by BitTorrent: each connection can check for itself whether
        // it's done, since this only depends on whether the last piece has 
        // been received (for which each connection is notified through the 
        // pieceReceived() call)
        throw new RuntimeException("not used by BitTorrent");
    }

    public void pieceReceived(Object origin, int pieceIndex) {
        // we received a new piece; notify our peer about it, unless our peer
        // sent it to us (in that case it already knows that we have this 
        // piece now)

        checkSendHave(origin, pieceIndex);
        checkMeDone();
        checkMeStop();
    }

    protected void checkSendHave(Object origin, int pieceIndex) {
        if (!origin.equals(peer)) {
            if (logger.isDebugEnabled()) {
                logger.debug("telling peer " + peer + " I have piece "
                        + pieceIndex);
            }
            asyncSender.enqueueHave(pieceIndex);
        }
    }

    public int getPiecesReceived() {
        return piecesReceived;
    }

    public int getPiecesSent() {
        return piecesSent;
    }

    public String getRateStats() {
        if (choking) {
            double upBytesNsec = communicator.getUploadBytesPerNanosec();
            double upMBSec = Convert.bytesPerNanosecToMBytesPerSec(upBytesNsec);
            String upRate = Convert.round(upMBSec, 2);

            double downBytesNsec = communicator.getDownloadBytesPerNanosec();
            double downMBSec = Convert.bytesPerNanosecToMBytesPerSec(downBytesNsec);
            String downRate = Convert.round(downMBSec, 2);

            return "upload_rate " + upRate + " download_rate " + downRate;
        } else {
            return "";
        }
    }

    public void printStats(long totalTimeMillis) {
        String percPiecesSent = Convert.round(piecesSent
                / (double) admin.getNoTotalPieces() * 100, 2);
        String percPiecesReceived = Convert.round(piecesReceived
                / (double) admin.getNoTotalPieces() * 100, 2);
        String percPiecePickTime = Convert.round(Convert
                .microsecToMillisec(piecePickTimer.totalTimeVal())
                / (double) totalTimeMillis * 100, 2);

        Config.statsLogger.info(me + " peer_stats " + peer + ": " + "sent " + 
                piecesSent + " = " + percPiecesSent + "% " + "rcvd " + 
                piecesReceived + " = " + percPiecesReceived + "% " + 
                "picking " + piecePickTimer.totalTime() + " = " + 
                percPiecePickTime + " %" + " " + getRateStats());

        communicator.printStats();
        asyncSender.printStats();
    }

    public void close() throws IOException {
        asyncSender.enqueueClose();
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (o instanceof BitTorrentConnection) {
            BitTorrentConnection other = (BitTorrentConnection) o;

            return peer.equals(other.peer);
        } else {
            return false;
        }
    }

    public String toString() {
        return "connection " + me + "<->" + peer;
    }

}
