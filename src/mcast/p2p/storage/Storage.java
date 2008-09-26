package mcast.p2p.storage;

import java.io.IOException;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

public interface Storage {

    /**
     * Returns a piece of data of this storage. This method must not block.
     * Availability of the data in the piece should be controlled elsewhere.
     * 
     * @param index
     *                the index of the desired piece
     * @return the piece, or <code>null</code> if no piece data is available
     */
    public Piece createPiece(int index) throws IOException;

    /**
     * Reads a piece out of the given message.
     * 
     * @param m
     *                the message to read a piece from
     * @return the read piece
     */
    public Piece readPiece(ReadMessage m) throws IOException;

    /**
     * Writes a piece to the given message.
     * 
     * @param m
     *                the message to write a piece to
     */
    public void writePiece(Piece piece, WriteMessage m) throws IOException;

    /**
     * Returns the number of pieces in this storage.
     * 
     * @return
     */
    public int getPieceCount();
    
    /**
     * Returns the average size of a piece (in bytes)
     */
    public int getAveragePieceSize();

    /**
     * Returns a unique digest value of this storage.
     * 
     * @return a unique digest value of this storage.
     */
    public byte[] getDigest() throws IOException;

    /**
     * Returns the size of this storage in bytes. Only used for testing purposes.
     */
    public long getByteSize() throws IOException;

    /**
     * Closes this storages, meaning no more pieces will be read out of it or
     * written into it. A storage implementation can use this method to free
     * resources.
     */
    public void close() throws IOException;

    /**
     * Sets all data in this storage to zero.
     */
    public void clear() throws IOException;

}
