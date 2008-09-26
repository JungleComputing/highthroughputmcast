package mcast.p2p.storage;

public class PieceFactory {

    private static final int PIECE_CACHE_SIZE = 256;

    private static class PieceCache {

        private PieceCache() {}

        static final Piece cache[] = new Piece[PIECE_CACHE_SIZE];

        static {
            for(int i = 0; i < cache.length; i++) {
                cache[i] = new PieceImpl(i);
            }
        }

    }

    public static Piece createPiece(int index) {
        if (index < PIECE_CACHE_SIZE) {
            return PieceCache.cache[index];
        }

        return new PieceImpl(index);
    }

}
