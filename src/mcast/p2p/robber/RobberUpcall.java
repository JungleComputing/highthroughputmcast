package mcast.p2p.robber;

import mcast.p2p.admin.PieceIndexSet;
import mcast.p2p.bittorrent.BitTorrentUpcall;

public interface RobberUpcall extends BitTorrentUpcall {

    void receiveDesire(PieceIndexSet pieceIndices);

    void receiveSteal();

    void receiveSteal(int peerPiecesReceived);

    void receiveWork(PieceIndexSet pieceIndices);

    void receiveFoundWork();

}
