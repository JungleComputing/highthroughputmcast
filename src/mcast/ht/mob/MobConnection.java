package mcast.ht.mob;

import ibis.ipl.IbisIdentifier;

import org.apache.log4j.Logger;

import mcast.ht.admin.PieceIndexSet;
import mcast.ht.bittorrent.BitTorrentAdmin;
import mcast.ht.bittorrent.BitTorrentConnection;
import mcast.ht.Collective;
import mcast.ht.Pool;
import mcast.ht.admin.P2PAdmin;
import mcast.ht.storage.Storage;

public class MobConnection extends BitTorrentConnection implements Config {

    private static Logger logger = Logger.getLogger(MobConnection.class);

    protected final Collective myCollective;
    protected final Collective peerCollective;
    protected volatile MobShare peerMobShare;

    public MobConnection(IbisIdentifier me, IbisIdentifier peer, Pool pool) {
        super(pool.getName(), me, peer, false, false, false);

        myCollective = pool.getCollective(me);
        peerCollective = pool.getCollective(peer);

        peerMobShare = null;
    }

    @Override
    public void init(Storage storage, P2PAdmin admin) {
        /**
         * First, determine what the mob share of my peer is
         */
        int totalPieces = storage.getPieceCount();
        peerMobShare = new MobShare(peerCollective, peer, totalPieces);

        if (logger.isDebugEnabled()) {
            logger.debug("mob share of " + peer + ": " + peerMobShare);
        }

        /**
         * Second, initialize the BitTorrent communication
         */
        super.init(storage, admin);
    }

    @Override
    protected PieceIndexSet getPiecesAvailableForPeer(BitTorrentAdmin admin) {
        if (peerCollective.equals(myCollective)) {
            // our peer is in the same cluster as we are; report all the pieces 
            // we have
            return admin.getPiecesReceived();
        } else {
            // our peer is in another cluster; only report the pieces our peer
            // is globally seeking as part of his mob share, so he'll only
            // request those from us.
            PieceIndexSet peerInterest = peerMobShare.getPieceIndices();
            PieceIndexSet possession = admin.getPiecesReceived();

            return peerInterest.and(possession);
        }
    }

    @Override
    protected void checkSendHave(Object origin, int pieceIndex) {
        // we received a new piece; notify our peer about it,
        // but only if he was not the one who sent it to us (in which case he
        // already knows that we have this piece now) and either the peer is in
        // the same cluster as we are, or the piece is part of his mob share.
        if (!origin.equals(peer) && 
            (peerCollective.equals(myCollective) || 
             peerMobShare.contains(pieceIndex))) 
        {
            asyncSender.enqueueHave(pieceIndex);
        }
    }

    @Override
    public void interested() {
        // MOB does not need interest state; do nothing
    }

    @Override
    public void notInterested() {
        // MOB does not need interest state; do nothing
    }

}
