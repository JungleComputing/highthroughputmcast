package mcast.ht.admin;

/**
 * Represents the interest of this peer in certain pieces from other peers.
 * The interest is split in two distinct groups: gold pieces and silver pieces.
 * Which pieces are gold and which ones are silver is set in the constructor.
 * The set of gold pieces can be changed using the setGoldPieces() method,
 * which can change gold pieces into silver ones and vice versa.
 *    
 * The interest can be informed of the possession of pieces on certain peers
 * by calling the tellHave() methods. We only remember the existence of pieces 
 * in which we are interested.   
 * 
 * The size of the interest never grows, it only shrinks.
 */
public interface PieceInterest {

    public static final int[] NOTHING = new int[0];

    /**
     * Notifies that a peer has a certain piece.
     * 
     * @param peer
     *                the peer that has a certain piece
     * @param pieceIndex
     *                the index of the piece that the peer has
     * @param wantGold
     *                whether we are only interested in a golden piece
     * 
     * @return true if we are interested in this piece, false otherwise. If
     *         wantOnlyGold is true, we only return true when we are interested 
     *         in this piece as a golden piece.
     */
    public boolean tellHave(Object peer, int pieceIndex, boolean wantOnlyGold);

    /**
     * Notifies that a peer has certain pieces
     * 
     * @param peer
     *                the peer that has certain pieces
     * @param pieceIndices
     *                the indices of the pieces that the peer has
     * @param wantOnlyGold
     *                whether we are only interested in golden pieces
     * 
     * @return true if we are interested in one or more of these pieces, false
     *         otherwise. If wantOnlyGold is true, we only return true when we 
     *         are interested in these pieces as golden pieces.
     */
    public boolean tellHave(Object peer, PieceIndexSet pieceIndices, 
            boolean wantOnlyGold);

    /**
     * Removes the given piece from this interest. All knowledge about which 
     * peers have this piece is also removed.
     * 
     * @param pieceIndex
     *                the index of the piece we're no longer interested in.
     */
    public void remove(int pieceIndex);

    /**
     * Removes at most the given amount of distinct golden or silver pieces from
     * this interest that the given peer has. Golden pieces are preferred; only
     * when the peer does not have any golden pieces left, silver ones are 
     * removed.
     * 
     * @param peer
     *                the peer whose possession of golden and silver piece we 
     *                should use
     * @param amount
     *                the maximum amount of pieces to remove
     * 
     * @return the indices of the removed pieces. The length of the returned
     *         array can vary between 0 (inclusive) and amount (exclusive).
     */
    public int[] removeGoldOrSilver(Object peer, int amount);

    /**
     * Removes at most the given amount of distinct golden pieces from this 
     * interest that the given peer has.
     * 
     * @param peer
     *                the peer whose possession of golden pieces we should use
     * @param amount
     *                the maximum amount of pieces to remove
     * 
     * @return the indices of the removed pieces. The length of the returned
     *         array can vary between 0 (inclusive) and amount (exclusive).
     */
    public int[] removeGold(Object peer, int amount);

    /**
     * Changes the metal of the given piece from gold to silver. If the piece
     * was already silver, or this interest does not contain the given piece,
     * nothing happens.
     * 
     * @param pieceIndex
     *                the piece to devaluate
     */
    public void devaluate(int pieceIndex);

    /**
     * Changes the metal of the given pieces from gold to silver. For pieces
     * that were already silver, or that are not part of this interest, nothing
     * happens.
     * 
     * @param pieceIndices
     *                the pieces to devaluate
     */
    public void devaluate(PieceIndexSet pieceIndices);

    /**
     * Changes the metal of the first fraction of golden pieces in this 
     * interest to silver.
     * 
     * @param fraction
     *		the fraction (between 0 and 1) of golden pieces to change to silver. 
     *		A fraction of 0.0 means no golden pieces at all, a fraction of 1.0 
     *		means all golden pieces.
     * 
     * @return the pieces that were devaluated from gold to silver.
     */
    public PieceIndexSet devaluateFirst(double fraction);

    /**
     * Changes the metal of the given pieces from silver to gold. For pieces
     * that were already gold, or that are not part of this interest, nothing
     * happens.
     * 
     * @param pieceIndices
     *                the pieces to revaluate
     */
    public void revaluate(PieceIndexSet pieceIndices);

    /**
     * Returns all the gold pieces a peer has.
     * 
     * @param peer the peer that has the golden pieces.
     * 
     * @return all the golden pieces from the given peer.
     */
    public PieceIndexSet getGold(Object peer);

    /**
     * Returns all the silver pieces a peer has.
     * 
     * @param peer the peer that has the silver pieces.
     * 
     * @return all the silver pieces from the given peer.
     */
    public PieceIndexSet getSilver(Object peer);

    /**
     * Returns all golden pieces in this interest.
     *  
     * @return all golden pieces in this interest.
     */
    public PieceIndexSet getGold();

    /**
     * Returns all silver pieces in this interest.
     *  
     * @return all silver pieces in this interest.
     */
    public PieceIndexSet getSilver();

    public boolean containsGold();

    public boolean containsSilver();

}
