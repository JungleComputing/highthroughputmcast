package mcast.p2p.robber;

import java.io.IOException;

import mcast.p2p.admin.PieceIndexSet;
import mcast.p2p.bittorrent.BitTorrentAsyncSender;
import mcast.p2p.util.Command;

public class RobberAsyncSender extends BitTorrentAsyncSender {

    private final RobberCommunicator communicator;
    private final SendSteal sendSteal;
    private final SendFoundWork sendFoundWork;

    public RobberAsyncSender(RobberCommunicator communicator) {
        super(communicator);

        this.communicator = communicator;

        sendSteal = new SendSteal();
        sendFoundWork = new SendFoundWork();
    }

    public synchronized void enqueueDesire(PieceIndexSet pieceIndices) {
        enqueue(new SendDesire(pieceIndices));
    }

    public synchronized void enqueueSendWork(PieceIndexSet pieceIndices) {
        enqueue(new SendWork(pieceIndices));
    }

    public synchronized void enqueueSendSteal() {
        enqueue(sendSteal);
    }

    public synchronized void enqueueSendFoundWork() {
        enqueue(sendFoundWork);
    }

    // INNER CLASSES

    private class SendDesire implements Command {

        private PieceIndexSet pieceIndices;

        SendDesire(PieceIndexSet pieceIndices) {
            this.pieceIndices = pieceIndices;
        }

        public void execute() throws IOException {
            communicator.sendDesire(pieceIndices);
        }
    }

    private class SendSteal implements Command {

        SendSteal() {
            // do nothing
        }

        public void execute() throws IOException {
            communicator.sendSteal();
        }
    }

    private class SendWork implements Command {

        private PieceIndexSet pieceIndices;

        SendWork(PieceIndexSet pieceIndices) {
            this.pieceIndices = pieceIndices;
        }

        public void execute() throws IOException {
            communicator.sendWork(pieceIndices);
        }
    }

    private class SendFoundWork implements Command {

        SendFoundWork() {
            // do nothing
        }

        public void execute() throws IOException {
            communicator.sendFoundWork();
        }
    }

}
