package mcast.ht.storage;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LongStorage implements Storage {

    private volatile long value;

    public LongStorage(long value) {
        this.value = value;
    }

    public long getValue() {
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
        return Long.SIZE;
    }

    public long getByteSize() throws IOException {
        return Long.SIZE;
    }

    public byte[] getDigest() throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            dos.writeLong(value);

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
        value = m.readLong();

        return PieceFactory.createPiece(0);
    }

    public void writePiece(Piece piece, WriteMessage m) throws IOException {
        m.writeLong(value);
    }

}
