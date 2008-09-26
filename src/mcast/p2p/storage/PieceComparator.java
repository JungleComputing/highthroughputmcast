package mcast.p2p.storage;

import java.util.Comparator;

class PieceComparator implements Comparator<Piece> {

    private static final PieceComparator uniqueInstance = new PieceComparator();

    protected PieceComparator() {
        // do nothing
    }

    public static PieceComparator getInstance() {
        return uniqueInstance;
    }

    public int compare(Piece p1, Piece p2) {
        return p1.getIndex() - p2.getIndex();
    }

}
