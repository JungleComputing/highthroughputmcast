package mcast.p2p.storage;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class StripedByteArrayStorage implements Storage {

    private final byte[][] data;
    private final int pieceSize;

    private int piecesPerStripe;

    public StripedByteArrayStorage(byte[][] data, int pieceSize) {
        this.data = checkData(data);
        this.pieceSize = pieceSize;

        piecesPerStripe = (int)Math.ceil(data[0].length / (double)pieceSize);
    }

    public StripedByteArrayStorage(StripedByteArrayStorage original) {
        data = new byte[original.data.length][];

        for (int i = 0; i < data.length; i++) {
            data[i] = new byte[original.data[i].length];
            System.arraycopy(original.data[i], 0, data[i], 0, original.data[i].length);
        }

        pieceSize = original.pieceSize;
        piecesPerStripe = original.piecesPerStripe;
    }

    byte[][] getData() {
        return data;
    }

    public void close() {
        // do nothing
    }

    public long getByteSize() {
        return ((data.length - 1) * data[0].length) + data[data.length - 1].length;
    }

    private byte[][] checkData(byte[][] data) {
        // check for null
        if (data == null) {
            throw new NullPointerException("data cannot be null");
        }

        // check zero stripes case: convert to correctly dimensioned array
        if (data.length == 0) {
            return new byte[1][0];
        }

        if (data.length > 0) {
            int stripeLength = data[0].length;

            // check that each stripe but the last one is as large as the first one
            for (int i = 1; i < data.length - 1; i++) {
                if (data[i].length != stripeLength) {
                    throw new IllegalArgumentException("length of data stripe (" + data[i].length + ") differs from previous stripe lengths (" + stripeLength + ")");
                }
            }

            // check that the last stripe is as big or smaller than the first one
            if (data.length > 1 && data[data.length - 1].length > stripeLength) {
                throw new IllegalArgumentException("length of the last data stripe (" + data[data.length - 1].length + ") is larger than other stripes (" + stripeLength + ")");
            }
        }

        return data;
    }

    public int getPieceCount() {
        return (piecesPerStripe * (data.length - 1)) + (int)Math.ceil(data[data.length - 1].length / (double)pieceSize);
    }

    public int getAveragePieceSize() {
        return pieceSize;
    }

    public Piece createPiece(int index) {
        return PieceFactory.createPiece(index);
    }

    public Piece readPiece(ReadMessage m) throws IOException {
        int index = m.readInt();

        int stripe = index / piecesPerStripe;
        int stripeOffset = (index - (stripe * piecesPerStripe)) * pieceSize;
        int pieceLength = Math.min(pieceSize, data[stripe].length - stripeOffset);

        m.readArray(data[stripe], stripeOffset, pieceLength);

        return new PieceImpl(index);
    }

    public void writePiece(Piece piece, WriteMessage m) throws IOException {
        int index = piece.getIndex();

        m.writeInt(index);

        int stripe = index / piecesPerStripe;
        int stripeOffset = (index - (stripe * piecesPerStripe)) * pieceSize;
        int pieceLength = Math.min(pieceSize, data[stripe].length - stripeOffset);

        m.writeArray(data[stripe], stripeOffset, pieceLength);
    }

    public int getByteSize(int index) {
        int stripe = index / piecesPerStripe;
        int stripeOffset = (index - (stripe * piecesPerStripe)) * pieceSize;

        return Math.min(pieceSize, data[stripe].length - stripeOffset);
    }

    public boolean equals(Object o) {
        if (o instanceof StripedByteArrayStorage) {
            StripedByteArrayStorage rhs = (StripedByteArrayStorage)o;

            if (pieceSize != rhs.pieceSize
                    || piecesPerStripe != rhs.piecesPerStripe
                    || data.length != rhs.data.length) {
                return false;
            }

            for (int i = 0; i < data.length; i++) {
                if (!Arrays.equals(data[i], rhs.data[i])) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public byte[] getDigest() {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            for (int i = 0; i < data.length; i++) {
                digest.update(data[i]);
            }

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("no such digest algorithm: MD5", e);
        }
    }

    public void clear() {
        for (int i = 0; i < data.length; i++) {
            Arrays.fill(data[i], (byte)0);
        }
    }

}
