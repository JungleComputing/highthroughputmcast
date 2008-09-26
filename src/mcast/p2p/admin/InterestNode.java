package mcast.p2p.admin;

class InterestNode {

    private final SharedPiece piece;
    private InterestNodeList owner;
    private InterestNode prev;
    private InterestNode next;

    InterestNode(SharedPiece piece, InterestNodeList owner) {
        this.piece = piece;
        this.owner = owner;
        prev = next = null;
    }

    SharedPiece getPiece() {
        return piece;
    }

    Object getPeer() {
        return owner.getPeer();
    }

    InterestNode getPrev() {
        return prev;
    }

    InterestNode getNext() {
        return next;
    }

    void setNext(InterestNode node) {
        next = node;
    }

    void setPrev(InterestNode node) {
        prev = node;
    }

    InterestNodeList getOwner() {
        return owner;
    }

    void setOwner(InterestNodeList newOwner) {
        if (owner == newOwner) {
            return;
        }

        delete();

        owner = newOwner;

        newOwner.addNode(this);
    }

    void delete() {
        if (owner.getFirst() == this) {
            owner.setFirst(next);
        }
        if (owner.getLast() == this) {
            owner.setLast(prev);
        }
        if (prev != null) {
            prev.next = next;
        }
        if (next != null) {
            next.prev = prev;
        }

        owner.decreaseSize();

        prev = next = null;
        owner = null;
    }
}
