package mcast.ht.storage;

import java.io.IOException;
import java.util.List;

public interface ConsecutivePiecesWriter {

    void writeConsecutivePieces(int firstPieceIndex, List<byte[]> bytes)
    throws IOException;

}
