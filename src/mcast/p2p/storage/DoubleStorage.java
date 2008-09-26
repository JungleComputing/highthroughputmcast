package mcast.p2p.storage;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DoubleStorage implements Storage {

    private volatile double value;

    public DoubleStorage(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void clear() throws IOException {
        value = 0;
    }

    public void close() {
        // do nothing
    }

    public Piece createPiece(int index) {
        return PieceFactory.createPiece(index);
    }

    public int getAveragePieceSize() {
        return Double.SIZE;
    }

    public long getByteSize() throws IOException {
        return Double.SIZE;
    }

    public byte[] getDigest() throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            dos.writeDouble(value);

            digest.update(bos.toByteArray());

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("no such digest algorithm: MD5", e);
        }
    }

    public int getPieceCount() {
        return 1;
    }

    public Piece readPiece(ReadMessage m) throws IOException {
        value = m.readDouble();

        return PieceFactory.createPiece(0);
    }

    public void writePiece(Piece piece, WriteMessage m) throws IOException {
        m.writeDouble(value);
    }

}
