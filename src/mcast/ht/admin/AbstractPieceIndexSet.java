package mcast.ht.admin;

public abstract class AbstractPieceIndexSet implements PieceIndexSet {

    public void addAll(PieceIndexSet set) {
        for (Integer i : set) {
            add(i);
        }
    }

    public boolean containsAny(PieceIndexSet pieceIndices) {
        if (isEmpty() || pieceIndices.isEmpty()) {
            return false;
        }

        PieceIndexSet smallest = null;
        PieceIndexSet largest = null;

        if (size() < pieceIndices.size()) {
            smallest = this;
            largest = pieceIndices;
        } else {
            smallest = pieceIndices;
            largest = this;
        }

        for (int i: smallest) {
            if (largest.contains(i)) {
                return true;
            }
        }

        return false;
    }

    public boolean removeAll(PieceIndexSet indices) {
        boolean result = false;

        for (int i: indices) {
            result |= remove(i);
        }

        return result;
    }

    public PieceIndexSet removeFirst(double fraction) {
        if (fraction == 0.0) {
            return createEmptySet();
        }
        if (fraction < 0.0) {
            throw new IllegalArgumentException("fraction ("  + fraction + 
            ") cannot be negative");
        }
        if (fraction > 1.0) {
            throw new IllegalArgumentException("fraction ("  + fraction + 
            ") cannot be more than 1.0");
        }

        int amount = (int)Math.floor(size() * fraction);

        if (amount == 0) {
            return createEmptySet();
        }

        return doRemoveFirst(amount);
    }

    protected abstract PieceIndexSet createEmptySet();

    protected abstract PieceIndexSet doRemoveFirst(int amount);

}
