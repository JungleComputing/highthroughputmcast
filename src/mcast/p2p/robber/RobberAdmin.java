package mcast.p2p.robber;

import ibis.ipl.IbisIdentifier;
import mcast.p2p.admin.PieceIndexSet;
import mcast.p2p.bittorrent.BitTorrentAdmin;

public interface RobberAdmin extends BitTorrentAdmin {

    public PieceIndexSet getDesire(IbisIdentifier peer);

    public boolean haveWorkForLocalPeer(IbisIdentifier peer);

    public PieceIndexSet stealWork(double fraction);

    public void giveWork(IbisIdentifier peer, PieceIndexSet work);

    public void peerFoundWork(IbisIdentifier peer);

}
