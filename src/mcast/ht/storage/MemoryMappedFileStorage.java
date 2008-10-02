package mcast.ht.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class MemoryMappedFileStorage extends AbstractFileStorage {

    private static final Logger logger = 
        Logger.getLogger(MemoryMappedFileStorage.class);

    private static MappedFileCache mappedFileCache = new MappedFileCache();

    private final int byteSize;

    private volatile int piecesWritten;

    public MemoryMappedFileStorage(File file, int byteSize, int pieceSize, 
            boolean readOnly) throws FileNotFoundException, IOException {
        super(file, pieceSize, readOnly);

        this.byteSize = byteSize;

        mappedFileCache = new MappedFileCache();
        piecesWritten = 0;
    }

    public void clear() throws IOException {
        piecesWritten = 0;
        mappedFileCache.removeMappedByteBuffer(file);
        super.clear();
    }

    public void close() throws IOException {
        mappedFileCache.removeMappedByteBuffer(file);
        super.close();
    }

    public long getByteSize() {
        return byteSize;
    }

    public void writeConsecutivePieces(int firstPieceIndex, List<byte[]> bytes)
            throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("writing " + bytes.size() + " pieces starting at #" + 
                    firstPieceIndex);
        }

        MappedByteBuffer buffer = mappedFileCache.getMappedByteBuffer(file, 0L, 
                byteSize);

        buffer.position(firstPieceIndex * pieceSize);

        for (byte[] piece: bytes) {
            buffer.put(piece);
        }

        buffer.force();

        piecesWritten += bytes.size();

        if (piecesWritten >= getPieceCount()) {
            mappedFileCache.removeMappedByteBuffer(file);
        }
    }

    // INNER CLASSES

    private static class MappedFileCache {

        private final LRUMappedByteBufferHashMap mappedFiles;

        MappedFileCache() {
            mappedFiles = new LRUMappedByteBufferHashMap();
        }

        public synchronized MappedByteBuffer getMappedByteBuffer(File file, 
                long position, int size) throws IOException {
            MappedByteBuffer result = mappedFiles.get(file);

            if (result == null) {
                OpenRandomAccessFileCache fc = OpenRandomAccessFileCache.getInstance();
                RandomAccessFile raf = fc.getRandomAccessFile(file, false);
                FileChannel channel = raf.getChannel();

                result = channel.map(FileChannel.MapMode.READ_WRITE, position, size);

                mappedFiles.put(file, result);
            }

            return result;
        }

        public synchronized void removeMappedByteBuffer(File file) {
            mappedFiles.remove(file);
        }

    }

    private static class LRUMappedByteBufferHashMap 
            extends LinkedHashMap<File, MappedByteBuffer> {

        private static final long serialVersionUID = 261707080650402521L;

        LRUMappedByteBufferHashMap() {
            super(128, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<File, MappedByteBuffer> eldest) {
            return MemoryUsage.used > MemoryUsage.MAX;
        }

    }

}
