package mcast.ht.storage;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.IOException;

import org.apache.log4j.Logger;

public class FakeStorage implements Storage {

    private static Logger logger = Logger.getLogger(FakeStorage.class);

    private final int byteSize;
    private final int noPieces;
    private final byte[] fakePiece;

    public FakeStorage(int byteSize, int pieceSize) {
        this.byteSize = byteSize;

        noPieces = (int)Math.ceil(byteSize / (float)pieceSize);

        logger.debug("created fake buffer of " + byteSize + " bytes = " + noPieces + " pieces");

        fakePiece = new byte[pieceSize];
    }

    public void clear() {
        // do nothing
    }

    public void close() {
        // do nothing
    }

    public Piece createPiece(int index) {
        return PieceFactory.createPiece(index);
    }

    public long getByteSize() {
        return byteSize;
    }

    public byte[] getDigest() {
        throw new UnsupportedOperationException("fake storage does not support digests");
    }

    public int getPieceCount() {
        return noPieces;
    }

    public int getAveragePieceSize() {
        return fakePiece.length;
    }

    public Piece readPiece(ReadMessage m) throws IOException {
        int index = m.readInt();
        int length = getByteSize(index);
        m.readArray(fakePiece, 0, length);

        return PieceFactory.createPiece(index);
    }

    public void writePiece(Piece piece, WriteMessage m) throws IOException {
        m.writeInt(piece.getIndex());
        int length = getByteSize(piece.getIndex());
        m.writeArray(fakePiece, 0, length);
    }

    public int getByteSize(int index) {
        if (index <= noPieces - 1) {
            return fakePiece.length;
        } else {
            // last piece
            int modulo = byteSize % fakePiece.length;

            if (modulo == 0) {
                // total bytes is exact a multiple of piece size, 
                // so the last piece has the same size as every other pieces 
                return fakePiece.length;
            } else {
                // the last piece is smaller
                return modulo;
            }
        }
    }

}
