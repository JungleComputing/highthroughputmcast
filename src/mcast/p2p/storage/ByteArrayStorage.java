package mcast.p2p.storage;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ByteArrayStorage implements Storage {

    private final byte[] data;
    private final int dataOffset;
    private final int dataLength;
    private final int pieceSize;

    public ByteArrayStorage(byte[] data, int dataOffset, int dataLength, int pieceSize) {
        this.data = data;
        this.dataOffset = dataOffset;
        this.dataLength = dataLength;
        this.pieceSize = pieceSize;
    }

    public ByteArrayStorage(ByteArrayStorage original) {
        data = new byte[original.data.length];
        System.arraycopy(original.data, 0, data, 0, original.data.length);

        dataOffset = original.dataOffset;
        dataLength = original.dataLength;
        pieceSize = original.pieceSize;
    }

    public byte[] getData() {
        return data;
    }

    public void close() {
        // do nothing
    }

    public int getPieceCount() {
        return (int)Math.ceil(dataLength / (double)pieceSize);
    }

    public int getAveragePieceSize() {
        return pieceSize;
    }

    public long getByteSize() {
        return dataLength;
    }

    public int getByteSize(int index) {
        int pieceOffset = dataOffset + (index * pieceSize);
        return Math.min(pieceSize, dataOffset + dataLength - pieceOffset);
    }

    public Piece createPiece(int index) {
        return PieceFactory.createPiece(index);
    }

    public Piece readPiece(ReadMessage m) throws IOException {
        int index = m.readInt();
        int length = getByteSize(index);
        m.readArray(data, dataOffset + (index * pieceSize), length);

        return PieceFactory.createPiece(index);
    }

    public void writePiece(Piece piece, WriteMessage m) throws IOException {
        m.writeInt(piece.getIndex());
        int length = getByteSize(piece.getIndex());
        m.writeArray(data, dataOffset + (piece.getIndex() * pieceSize), length);
    }

    public byte[] getDigest() {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            digest.update(data, dataOffset, dataLength);

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("no such digest algorithm: MD5", e);
        }
    }

    public void clear() {
        Arrays.fill(data, dataOffset, dataOffset + dataLength, (byte)0);
    }

    public String toString() {
        return Arrays.toString(data);
    }

}
