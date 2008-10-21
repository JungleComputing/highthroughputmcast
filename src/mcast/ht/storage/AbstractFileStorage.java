package mcast.ht.storage;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

/**
 * @author mathijs
 */
public abstract class AbstractFileStorage 
implements VerifiableStorage, ConsecutivePiecesWriter {

    protected static Logger logger = Logger.getLogger(RandomAccessFileStorage.class);

    protected final File file;
    protected final int pieceSize;
    protected final boolean readOnly;

    protected final WriteCache writeCache;
    protected volatile int piecesReceived;

    public AbstractFileStorage(File file, int pieceSize, boolean readOnly) 
    throws FileNotFoundException, IOException 
    {
        this.file = file;
        this.pieceSize = pieceSize;
        this.readOnly = readOnly;

        if (!file.exists()) {
            // assumption: we are not a seed node
            // create the empty file and its parent directories

            File parentDir = file.getParentFile();
            if (parentDir != null) {
                parentDir.mkdirs();
            }

            file.createNewFile();
        }

        writeCache = new WriteCache(this);
        piecesReceived = 0;
    }

    public void clear() throws IOException {
        close();
        file.delete();
        file.createNewFile();
    }

    public void close() 
    throws IOException
    {
        OpenRandomAccessFileCache.getInstance().closeRandomAccessFile(file);
        writeCache.clear();
    }

    public Piece createPiece(int index) {
        return PieceFactory.createPiece(index);
    }

    public byte[] getDigest() throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            byte[] buf = new byte[pieceSize];

            RandomAccessFile raf = OpenRandomAccessFileCache.getInstance().getRandomAccessFile(file, readOnly); 

            synchronized(raf) {
                raf.seek(0);

                while (raf.read(buf) >= 0) {
                    digest.update(buf);
                }
            }

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("no such digest algorithm: MD5", e);
        }
    }

    public int getPieceCount() {
        return (int)Math.ceil(getByteSize() / (double)pieceSize);
    }

    public Piece readPiece(ReadMessage m) throws IOException {
        int index = m.readInt();

        if (index < 0) {
            throw new IOException("read negative piece index: " + index);
        }

        int length = getByteSize(index);

        byte[] buf = new byte[length];
        m.readArray(buf);

        Piece piece = createPiece(index);

        if (logger.isTraceEnabled()) {
            logger.trace("received piece " + piece);
        }

        writeCache.addPiece(piece, buf);

        piecesReceived++;

        return piece;
    }

    public void writePiece(Piece piece, WriteMessage m) throws IOException {
        int index = piece.getIndex();
        int length = getByteSize(index);

        // try reading the piece from the write cache
        byte[] buf = writeCache.getCachedPiece(piece);

        if (buf == null) {
            // the piece was already written to disk; read it again

            buf = new byte[length];

            RandomAccessFile raf = OpenRandomAccessFileCache.getInstance().getRandomAccessFile(file, readOnly);

            synchronized(raf) {
                raf.seek(index * pieceSize);
                raf.readFully(buf);
            }
        }

        // write the piece into the message
        m.writeInt(index);
        m.writeArray(buf);
    }

}
