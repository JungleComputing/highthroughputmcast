package mcast.ht.robber;

import ibis.ipl.IbisIdentifier;
import mcast.ht.admin.P2PAdmin;
import mcast.ht.admin.PieceIndexSet;
import mcast.ht.admin.PieceIndexSetFactory;
import mcast.ht.admin.SynchronizedPieceIndexSet;
import mcast.ht.bittorrent.BitTorrentAdmin;
import mcast.ht.bittorrent.BitTorrentAsyncSender;
import mcast.ht.bittorrent.BitTorrentCommunicator;
import mcast.ht.bittorrent.BitTorrentConnection;
import mcast.ht.storage.Piece;
import mcast.ht.storage.Storage;

public class RobberConnection extends BitTorrentConnection 
implements RobberUpcall, Config {

    private final SynchronizedPieceIndexSet peerDesire;
    private volatile RobberCommunicator communicator;
    private volatile RobberAsyncSender asyncSender;
    private volatile RobberAdmin robberAdmin;

    public RobberConnection(String poolName, IbisIdentifier me, 
            IbisIdentifier peer) {
        super(poolName, me, peer, false, false, false);

        PieceIndexSet nothing = PieceIndexSetFactory.createEmptyPieceIndexSet();
        peerDesire = new SynchronizedPieceIndexSet(nothing);
    }

    @Override
    protected BitTorrentCommunicator createCommunicator(IbisIdentifier me, 
            IbisIdentifier peer, boolean estimateDownloadRate, 
            boolean estimateUploadRate) {
        communicator = new RobberCommunicator(me, peer, this);

        return communicator;
    }

    @Override
    protected BitTorrentAsyncSender createAsyncSender() {
        asyncSender = new RobberAsyncSender(communicator);

        return asyncSender;
    }

    @Override
    public synchronized void init(Storage storage, P2PAdmin admin) {
        robberAdmin = (RobberAdmin)admin;

        super.init(storage, admin);

        // initially, we assume our peer desires nothing
        peerDesire.clear();
    }

    @Override
    public void startCommunication() {
        PieceIndexSet desire = robberAdmin.getDesire(peer);
        sendDesire(desire);

        if (robberAdmin.haveWorkForLocalPeer(peer)) {
            sendFoundWork();
        }
    }

    public void sendDesire(PieceIndexSet indices) {
        asyncSender.enqueueDesire(indices);
    }

    @Override
    public void interested() {
        // Currently, Robber does not use interest state; do nothing
    }

    @Override
    public void notInterested() {
        // Currently, Robber does not use interest state; do nothing
    }

    @Override
    public void receiveHave(int pieceIndex) {
        // now that our peer has a piece, it obviously no longer desires it
        peerDesire.remove(pieceIndex);

        super.receiveHave(pieceIndex);
    }

    @Override
    public void receiveBitfield(PieceIndexSet pieceIndices) {
        // now that our peer has certain pieces, it obviously no longer desires them
        peerDesire.removeAll(pieceIndices);

        super.receiveBitfield(pieceIndices);
    }

    @Override
    protected void checkSendHave(Object origin, int pieceIndex) {
        // we received a new piece; notify our peer about it,
        // but only if he was not the one who sent it to us (in which case he
        // already knows that we have this piece now) and the piece is part
        // of the peer's desire

        if (!origin.equals(peer)) {
            if (peerDesire.contains(pieceIndex)) {
                asyncSender.enqueueHave(pieceIndex);
            }
        }
    }

    public void stealWork() {
        asyncSender.enqueueSendSteal();
    }

    public void sendFoundWork() {
        asyncSender.enqueueSendFoundWork();
    }

    public void receiveDesire(PieceIndexSet pieceIndices) {
        // update our peer's desire
        peerDesire.init(pieceIndices);

        // report the pieces we have available for our peer
        PieceIndexSet available = getPiecesAvailableForPeer(robberAdmin);

        if (!available.isEmpty()) {
            asyncSender.enqueueBitfield(available);
        }
    }

    @Override
    protected PieceIndexSet getPiecesAvailableForPeer(BitTorrentAdmin admin) {
        // report all the pieces we possess and our peer desires

        PieceIndexSet possession = admin.getPiecesReceived();

        return peerDesire.and(possession);
    }

    @Override
    protected void sendPieceToPeer(Piece piece) {
        super.sendPieceToPeer(piece);

        peerDesire.remove(piece.getIndex());
    }

    public void receiveSteal() {
        doSteal(0.5);
    }

    public void receiveSteal(int peerPiecesReceived) {
        if (peerPiecesReceived == 0) {
            doSteal(0.5);
        } else {
            int myPiecesReceived = robberAdmin.getPiecesReceivedCount();
            double togetherPiecesReceived = myPiecesReceived + peerPiecesReceived;
            double fraction = peerPiecesReceived / togetherPiecesReceived;
            doSteal(fraction);
        }
    }

    private void doSteal(double fraction) {
        PieceIndexSet booty = robberAdmin.stealWork(fraction);

        asyncSender.enqueueSendWork(booty);
    }

    public void receiveWork(PieceIndexSet work) {
        robberAdmin.giveWork(peer, work);
    }

    public void receivedWork() {
        // We received new work; try to request more pieces from our peer
        requestMorePieces();
    }

    public void receiveFoundWork() {
        robberAdmin.peerFoundWork(peer);
    }

}
