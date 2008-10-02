package mcast.ht.admin;

import ibis.ipl.ReadMessage;

import java.io.IOException;

public class PieceIndexSetFactory {

    public static PieceIndexSet createEmptyPieceIndexSet() {
        return new PieceIndexBooleanSet();
    }

    public static PieceIndexSet createEmptyPieceIndexSet(int capacity) {
        return new PieceIndexBooleanSet(capacity); 
    }

    public static PieceIndexSet createFullPieceIndexSet(int size) {
        PieceIndexSet set = new PieceIndexBooleanSet(size);
        set.init(0, size);
        return set;
    }

    public static PieceIndexSet readPieceIndexSet(ReadMessage m) throws IOException {
        return new PieceIndexBooleanSet(m);
    }

}
