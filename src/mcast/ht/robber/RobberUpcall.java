package mcast.ht.robber;

import mcast.ht.admin.PieceIndexSet;
import mcast.ht.bittorrent.BitTorrentUpcall;

public interface RobberUpcall extends BitTorrentUpcall {

    void receiveDesire(PieceIndexSet pieceIndices);

    void receiveSteal();

    void receiveSteal(int peerPiecesReceived);

    void receiveWork(PieceIndexSet pieceIndices);

    void receiveFoundWork();

}
