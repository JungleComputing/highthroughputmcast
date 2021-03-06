package mcast.ht.bittorrent;

import mcast.ht.admin.PieceIndexSet;
import mcast.ht.storage.Piece;

/**
 * @author mathijs
 */
public interface BitTorrentUpcall {

    void receiveChoke();

    void receiveUnchoke();

    void receiveInterested();

    void receiveNotInterested();

    void receiveHave(int pieceIndex);

    void receiveBitfield(PieceIndexSet pieceIndices);

    void receiveRequest(int pieceIndex);

    void receivePiece(Piece piece);

    void receiveCancel(int pieceIndex);

    void receiveDone();

    void receiveStop();

}
