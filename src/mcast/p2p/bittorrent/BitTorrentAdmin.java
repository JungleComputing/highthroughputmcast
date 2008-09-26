package mcast.p2p.bittorrent;

import ibis.ipl.IbisIdentifier;

import java.io.IOException;

import mcast.p2p.admin.P2PAdmin;
import mcast.p2p.admin.PieceIndexSet;
import mcast.p2p.storage.Piece;

public interface BitTorrentAdmin extends P2PAdmin {

    public int getNoTotalPieces();

    public boolean isPieceReceived(int index);

    public boolean addExistence(Object peer, int pieceIndex);

    public boolean addExistence(Object peer, PieceIndexSet pieceIndices);

    public int[] requestDesiredPieceIndices(Object peer, int amount);

    public PieceIndexSet getPiecesReceived();

    public int getPiecesReceivedCount();

    public void setPieceReceived(IbisIdentifier origin, Piece piece);

    public boolean areAllPieceReceived();

    public void waitUntilAllPiecesReceived();

    public void printStats() throws IOException;

}
