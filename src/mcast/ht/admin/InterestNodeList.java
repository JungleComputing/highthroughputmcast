package mcast.ht.admin;

class InterestNodeList {

    private final Object peer;
    private InterestNode first, last;
    private int size;

    InterestNodeList(Object peer) {
        this.peer = peer;
        first = last = null;
        size = 0;
    }

    Object getPeer() {
        return peer;
    }

    InterestNode getFirst() {
        return first;
    }

    InterestNode getLast() {
        return last;
    }

    void setFirst(InterestNode node) {
        first = node;
    }

    void setLast(InterestNode node) {
        last = node;
    }

    void addPiece(SharedPiece piece) {
        InterestNode node = new InterestNode(piece, this);

        if (piece.addNode(node)) {
            addNode(node);
        }
    }

    void addNode(InterestNode node) {
        if (first == null) {
            first = last = node;
        } else {
            node.setNext(first);
            first.setPrev(node);
            first = node;
        }

        size++;
    }

    int size() {
        return size;
    }

    void decreaseSize() {
        size--;
    }

    InterestNode getNode(int index) {
        if (index < (size / 2)) {
            // seek from the first
            InterestNode pivot = first;

            for (int i = 0; i < index; i++) {
                pivot = pivot.getNext();
            }

            return pivot;
        } else {
            // seek from the last
            InterestNode pivot = last;

            for (int i = size - 1; i > index; i--) {
                pivot = pivot.getPrev();
            }

            return pivot;
        }
    }

    PieceIndexSet getPieceIndices() {
        PieceIndexSet result = PieceIndexSetFactory
        .createEmptyPieceIndexSet(size);

        for (InterestNode pivot = first; pivot != null; pivot = pivot.getNext()) {
            result.add(pivot.getPiece().getIndex());
        }

        return result;
    }

}
