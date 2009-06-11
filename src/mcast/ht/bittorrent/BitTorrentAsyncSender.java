package mcast.ht.bittorrent;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import mcast.ht.admin.PieceIndexSet;
import mcast.ht.admin.PieceIndexSetFactory;
import mcast.ht.storage.Piece;
import mcast.ht.util.Command;

import org.apache.log4j.Logger;

public class BitTorrentAsyncSender implements Runnable, Config {

    private static Logger logger = 
        Logger.getLogger(BitTorrentAsyncSender.class);

    private final BitTorrentCommunicator communicator;
    protected final LinkedBlockingQueue<Command> queue;
    private final SendInterested sendInterested;
    private final SendNotInterested sendNotInterested;
    private final SendChoke sendChoke;
    private final SendUnchoke sendUnchoke;
    private final SendDone sendDone;
    private final SendStop sendStop;
    private final Close close;
    private boolean done;
    private volatile int flushId, callbackId;
    private final PieceIndexSet cancelledPieces;
    private volatile int cancelledCount;

    public BitTorrentAsyncSender(BitTorrentCommunicator communicator) {
        this.communicator = communicator;

        queue = new LinkedBlockingQueue<Command>();

        sendInterested = new SendInterested();
        sendNotInterested = new SendNotInterested();
        sendChoke = new SendChoke();
        sendUnchoke = new SendUnchoke();
        sendDone = new SendDone();
        sendStop = new SendStop();

        close = new Close();

        done = false;
        flushId = 0;
        callbackId = 0;

        cancelledPieces = PieceIndexSetFactory.createEmptyPieceIndexSet();
        cancelledCount = 0;
    }

    public synchronized void enqueueBitfield(PieceIndexSet pieceIndices) {
        enqueue(new SendBitfield(pieceIndices));
    }

    public synchronized void enqueueRequest(int[] pieceIndices) {
        enqueue(new SendRequest(pieceIndices));
    }

    public synchronized void enqueuePiece(Piece piece) {
        enqueue(new SendPiece(piece));
    }

    public void cancel(int pieceIndex) {
        synchronized (cancelledPieces) {
            cancelledPieces.add(pieceIndex);
        }
    }

    public synchronized void enqueueHave(int pieceIndex) {
        enqueue(new SendHave(pieceIndex));
    }

    public synchronized void enqueueCancel(int pieceIndex) {
        enqueue(new SendCancel(pieceIndex));
    }

    public void enqueueInterested() {
        enqueue(sendInterested);
    }

    public void enqueueNotInterested() {
        enqueue(sendNotInterested);
    }

    public void enqueueChoke() {
        enqueue(sendChoke);
    }

    public void enqueueUnchoke() {
        enqueue(sendUnchoke);
    }

    public void enqueueDone() {
        enqueue(sendDone);
    }

    public void enqueueStop() {
        enqueue(sendStop);
    }

    public synchronized void enqueueClose() {
        enqueue(close);
    }

    protected void enqueue(Command c) {
        try {
            queue.put(c);
        } catch (InterruptedException e) {
            logger.fatal("Interrupted while enqueueing command", e);
        }
    }

    public synchronized void flush() {
        if (queue.isEmpty()) {
            return;
        } else {
            flushId++;

            int myFlushId = flushId;

            enqueue(new Callback());

            while (callbackId != myFlushId) {
                try {
                    logger.debug("Waiting for callback of flush command...");
                    wait();
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting for callback of " +
                    "flush command");
                }
            }
        }
    }

    private synchronized void callback() {
        callbackId++;
        notifyAll();
    }

    public synchronized void printStats(String prefix) {
        if (END_GAME) {
            Config.statsLogger.info(prefix + "#cancelled pieces: " + 
                    cancelledCount);
        }
        cancelledCount = 0;
    }

    public void run() {
        while (!done) {
            try {
                Command command = queue.take();
                command.execute();
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for next command", e);
            } catch (IOException e) {
                logger.error("problem while executing command", e);
            }
        }
    }

    private class SendBitfield implements Command {

        private PieceIndexSet pieceIndices;

        SendBitfield(PieceIndexSet pieceIndices) {
            this.pieceIndices = pieceIndices;
        }

        public void execute() throws IOException {
            communicator.sendBitfield(pieceIndices);
        }
    }

    private class SendInterested implements Command {

        SendInterested() {
            // do nothing
        }

        public void execute() throws IOException {
            communicator.sendInterested();
        }

    }

    private class SendNotInterested implements Command {

        SendNotInterested() {
            // do nothing
        }

        public void execute() throws IOException {
            communicator.sendNotInterested();
        }

    }

    private class SendChoke implements Command {

        SendChoke() {
            // do nothing
        }

        public void execute() throws IOException {
            communicator.sendChoke();
        }

    }

    private class SendUnchoke implements Command {

        SendUnchoke() {
            // do nothing
        }

        public void execute() throws IOException {
            communicator.sendUnchoke();
        }

    }

    private class SendRequest implements Command {

        private int[] pieceIndices;

        SendRequest(int[] pieceIndices) {
            this.pieceIndices = pieceIndices;
        }

        public void execute() throws IOException {
            for (int i = 0; i < pieceIndices.length; i++) {
                communicator.sendRequest(pieceIndices[i]);
            }
        }

    }

    private class SendPiece implements Command {

        private Piece piece;

        SendPiece(Piece piece) {
            this.piece = piece;
        }

        public void execute() throws IOException {
            synchronized (cancelledPieces) {
                if (cancelledPieces.remove(piece.getIndex())) {
                    cancelledCount++;
                    return;
                }
            }
            communicator.sendPiece(piece);
        }

    }

    private class SendHave implements Command {

        private int pieceIndex;

        SendHave(int pieceIndex) {
            this.pieceIndex = pieceIndex;
        }

        public void execute() throws IOException {
            communicator.sendHave(pieceIndex);
        }

    }

    private class SendCancel implements Command {

        private int pieceIndex;

        SendCancel(int pieceIndex) {
            this.pieceIndex = pieceIndex;
        }

        public void execute() throws IOException {
            communicator.sendCancel(pieceIndex);
        }

    }

    private class SendDone implements Command {

        SendDone() {
            // do nothing
        }

        public void execute() throws IOException {
            communicator.sendDone();
        }

    }

    private class SendStop implements Command {

        SendStop() {
            // do nothing
        }

        public void execute() throws IOException {
            communicator.sendStop();
        }

    }

    private class Close implements Command {

        Close() {
            // do nothing
        }

        public void execute() throws IOException {
            communicator.close();
            done = true;
        }

    }

    private class Callback implements Command {

        Callback() {
            // do nothing
        }

        public void execute() {
            callback();
        }
    }

}
