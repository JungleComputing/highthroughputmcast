package mcast.p2p.admin;

import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.Iterator;

public interface PieceIndexSet extends Iterable<Integer> {

    /**
     * Fills this set with amount consecutive piece indices starting at offset.
     * All other existing piece indices are removed.
     * 
     * @param offset
     *                the first piece index to add
     * @param length
     *                the amount of piece indices to add, starting at the offset
     */
    public void init(int offset, int amount);

    /**
     * Removes all piece indices in this set.
     */
    public void clear();

    /**
     * Checks whether this set contains the given piece index.
     * 
     * @param index
     *                the piece index to check
     * @return true if this set contains the given piece index, false otherwise.
     */
    public boolean contains(int index);


    /**
     * Checks whether this set contains any of the gives piece indices.
     * 
     * @param pieceIndices
     *                the indices to check
     * 
     * @return true if this set contains one or more of the given piece indices,
     *         false otherwise.
     */
    public boolean containsAny(PieceIndexSet pieceIndices);

    /**
     * Checks whether this set contains no piece indices.
     * 
     * @return true if this set does not contain any piece indices, false
     *         otherwise.
     */
    public boolean isEmpty();

    /**
     * Adds the given piece index to this set. If the set already contains the
     * index, nothing happens.
     * 
     * @param index
     *                the piece index to add
     */
    public void add(int index);

    /**
     * Adds all the piece indices from start (inclusive) to end (exclusive) to
     * this set.
     * 
     * @param indices
     *                the set of piece indices to add
     * @param start
     *                the first piece index to add (inclusive)
     * @param end
     *                the last piece index to add (exclusive)
     */
    public void add(int start, int end);

    /**
     * Adds all the given piece indices to this set. Piece indices that this set
     * already contains are skipped.
     * 
     * @param indices
     *                the set indicating the piece indices to add
     */
    public void addAll(PieceIndexSet indices);

    /**
     * Removes a piece index from this set. If the set does not contain the
     * index, nothing happens.
     * 
     * @param index
     *                the piece index to remove.
     * @return <code>true</code> if the index was actually removed,
     *         <code>false</code> if nothing happened.
     */
    public boolean remove(int index);

    /**
     * Removes all the given piece indices from this set.
     * 
     * @param indices the indices to remove from this set.
     * 
     * @return true if one or more indices were actually removed, false otherwise.
     */
    public boolean removeAll(PieceIndexSet indices);

    /**
     * Removes the first given fraction of this piece index set. The fraction must be 
     * rounded down to an integral number of pieces (you remove half a piece index).
     * 
     * @param fraction
     * 		the fraction of piece indices to remove. The value should lie between
     * 		0.0 (inclusive) and 1.0 (inclusive). A fraction of 0.0 means no piece
     * 		indices will be removed from this set, a fraction of 1.0 means all 
     * 		piece indices will be removed from this set.
     * 
     * @return a new piece index set that contains the first given fraction of the pieces 
     * in this set
     */
    public PieceIndexSet removeFirst(double fraction);

    /**
     * Returns the number of piece indices in this set.
     * 
     * @return the number of piece indices in this set.
     */
    public int size();

    /**
     * Returns a new piece indices object that contains all indices that are
     * present in this object and the given one.
     */
    public PieceIndexSet and(PieceIndexSet other);

    /**
     * Returns a new piece indices object that contains all indices that are
     * present in either this object or the given one.
     */
    public PieceIndexSet or(PieceIndexSet other);

    /**
     * Returns a new piece indices object that contains all indices from 0 to
     * lastIndex (exclusive) that are <i>not</i> present in this object.
     */
    public PieceIndexSet not(int lastIndex);

    /**
     * Writes these piece indices to a write message; how to read them back
     * depends on the implementation :).
     * 
     * @param m
     *                the message to write these piece indices to.
     * 
     * @throws IOException
     */
    public void writeTo(WriteMessage m) throws IOException;

    /**
     * Returns an iterator of Integer objects, that each represent the index of
     * a piece that is present in this set.
     * 
     * @return an iterator of Integer objects
     */
    public Iterator<Integer> iterator();

    /**
     * Returns a copy of this set.
     */
    public PieceIndexSet deepCopy();

}
