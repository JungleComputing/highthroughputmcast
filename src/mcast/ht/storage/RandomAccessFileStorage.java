package mcast.ht.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import org.apache.log4j.Logger;

public class RandomAccessFileStorage extends AbstractFileStorage {

    private static Logger logger = 
        Logger.getLogger(RandomAccessFileStorage.class);

    private final long byteSize;
    
    public RandomAccessFileStorage(File file, long byteSize, int pieceSize, 
            boolean readOnly) throws FileNotFoundException, IOException {
        super(file, pieceSize, readOnly);

        this.byteSize = byteSize;
    }

    public long getByteSize() {
        return byteSize;
    }

    public void writeConsecutivePieces(int firstPieceIndex, List<byte[]> bytes)
            throws IOException {
        OpenRandomAccessFileCache fc = OpenRandomAccessFileCache.getInstance();
        RandomAccessFile raf = fc.getRandomAccessFile(file, readOnly);

        if (logger.isDebugEnabled()) {
            logger.debug("writing " + bytes.size() + " pieces starting at #" + 
                    firstPieceIndex);
        }

        synchronized(raf) {
            raf.seek(firstPieceIndex * pieceSize);

            for (byte[] buf: bytes) {
                raf.write(buf);
            }
        }
    }

}
