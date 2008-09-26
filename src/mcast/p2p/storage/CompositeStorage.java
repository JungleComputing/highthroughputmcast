package mcast.p2p.storage;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

public class CompositeStorage implements Storage {

    private final LinkedList<Storage> storages;

    public CompositeStorage() {
        storages = new LinkedList<Storage>();
    }

    public void addStorage(Storage s) {
        storages.add(s);
    }

    public void clear() 
    throws IOException 
    {
        for (Storage s: storages) {
            s.clear();
        }
    }

    public void close() 
    throws IOException
    {
        for (Storage s: storages) {
            s.close();
        }
    }

    public Piece createPiece(int index) {
        return PieceFactory.createPiece(index);
    }

    public int getAveragePieceSize() {
        if (storages.isEmpty()) {
            return 0;
        }

        int sum = 0;

        for (Storage s: storages) {
            sum += s.getAveragePieceSize();
        }

        return sum / storages.size();
    }

    public long getByteSize() throws IOException {
        int result = 0;

        for (Storage s: storages) {
            result += s.getByteSize();
        }

        return result;
    }

    public byte[] getDigest() throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            for (Storage s: storages) {
                digest.update(s.getDigest());
            }

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("no such digest algorithm: MD5", e);
        }
    }

    public int getPieceCount() {
        int result = 0;

        for (Storage s: storages) {
            result += s.getPieceCount();
        }

        return result;
    }

    public Piece readPiece(ReadMessage m) throws IOException {
        int pieceIndex = m.readInt();

        Position pos = getPosition(pieceIndex);

        if (pos == null) {
            throw new IOException("piece " + pieceIndex + " is not part of this multi-file storage (which contains " + getPieceCount() + " pieces)");
        }

        pos.storage.readPiece(m);

        return PieceFactory.createPiece(pieceIndex);
    }

    public void writePiece(Piece piece, WriteMessage m) 
    throws IOException 
    {
        Position pos = getPosition(piece.getIndex());

        if (pos == null) {
            throw new IOException("piece " + piece.getIndex() + " is not part of this multi-file storage (which contains " + getPieceCount() + " pieces)");
        }

        m.writeInt(piece.getIndex());

        Piece relativePiece = PieceFactory.createPiece(pos.pieceIndex);
        pos.storage.writePiece(relativePiece, m);
    }

    private Position getPosition(int pieceIndex) {
        int offset = 0;

        for (Storage s: storages) {
            int pieceCount = s.getPieceCount();

            offset += pieceCount;

            if (pieceIndex < offset) {
                int relativePieceIndex = pieceIndex - offset + pieceCount;
                return new Position(s, relativePieceIndex);
            }
        }

        return null;
    }

    // INNER CLASSES

    private class Position {

        Storage storage;
        int pieceIndex;

        Position(Storage storage, int pieceIndex) {
            this.storage = storage;
            this.pieceIndex = pieceIndex;
        }
    }

}
