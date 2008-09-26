package mcast.p2p.storage;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

public class WriteCache {

    private static final Logger logger = Logger.getLogger(WriteCache.class);

    private final ConsecutivePiecesWriter writer;
    private final TreeMap<Piece, byte[]> cache;
    private final TreeSet<Piece> alreadyWritten;
    private volatile int nextPiece;

    public WriteCache(ConsecutivePiecesWriter writer) {
        this.writer = writer;

        cache = new TreeMap<Piece, byte[]>(PieceComparator.getInstance());
        alreadyWritten = new TreeSet<Piece>(PieceComparator.getInstance());
        nextPiece = 0;
    }

    public synchronized void clear() {
        cache.clear();
        alreadyWritten.clear();
        nextPiece = 0;
    }

    public synchronized void addPiece(Piece piece, byte[] buf) 
    throws IOException
    {
        cache.put(piece, buf);

        MemoryUsage.used += buf.length;

        if (logger.isDebugEnabled()) {
            logger.debug("cache size: " + cache.size() + " pieces, mem. used: " + MemoryUsage.used + " bytes");
        }

        if (piece.getIndex() == nextPiece) {
            // we received The Next Piece!
            // write all consecutive pieces in cache to disk 
            writeCachedPiecesToFile(nextPiece);
        } else if (MemoryUsage.used > MemoryUsage.MAX){
            // we are running low on memory; flush the first piece and all successive ones
            logger.debug("low memory detected, flushing write cache");
            writeCachedPiecesToFile(-1);
        }
    }

    public synchronized byte[] getCachedPiece(Piece piece) {
        return cache.get(piece);
    }

    private void writeCachedPiecesToFile(int startIndex)
    throws IOException
    {
        if (cache.isEmpty()) {
            return;
        }

        Piece first = cache.firstKey();

        if (startIndex < 0) {
            // start index unspecified; take the first consecutive set of pieces
            startIndex = first.getIndex();
        } else if (first.getIndex() != startIndex) {
            // start index specified; if the first piece does not match the requested one, we quit
            return;
        }

        // check if we should remember the written pieces.
        // this is the case when we write a set of pieces that does not start at the 'nextPieceInFile' offset
        boolean rememberWrites = (startIndex != nextPiece);

        List<byte[]> bytes = new LinkedList<byte[]>();

        int index = startIndex;

        for (Iterator<Map.Entry<Piece, byte[]>> it = cache.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Piece, byte[]> entry = it.next();

            Piece piece = entry.getKey();

            if (piece.getIndex() == index) {
                byte[] buf = entry.getValue();

                bytes.add(buf);

                it.remove();

                MemoryUsage.used -= buf.length;

                if (rememberWrites) {
                    alreadyWritten.add(piece);
                }

                index++;
            } else {
                break;
            }
        }

        writer.writeConsecutivePieces(startIndex, bytes);

        if (!rememberWrites) {
            // we just wrote a serie of piece starting at the file pointer 'nextPiece'
            // first, move the pointer to the gap at the end of the series of pieces we just wrote
            nextPiece = index;

            // next, check if the next gap consists of pieces that were
            // written before in an attempt to free more memory. In that case, we have to
            // move the file pointer beyond the first consequtive serie of already written pieces
            if (!alreadyWritten.isEmpty()) {
                for (Iterator<Piece> it = alreadyWritten.iterator(); it.hasNext(); ) {
                    Piece written = it.next();

                    if (written.getIndex() == nextPiece) {
                        it.remove();
                        nextPiece++;
                    } else {
                        return;
                    }
                }
            }
        }
    }

}
