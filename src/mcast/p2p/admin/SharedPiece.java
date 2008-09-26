package mcast.p2p.admin;

import java.util.LinkedList;
import java.util.List;

class SharedPiece {

    private final int index;
    private final List<InterestNode> nodes;

    SharedPiece(int index) {
        this.index = index;
        nodes = new LinkedList<InterestNode>();
    }

    int getIndex() {
        return index;
    }

    boolean addNode(InterestNode newNode) {
        // first, check if the new node is not already present in one of our lists
        // if so, the node is a duplicate and should not be added
        for (InterestNode node: nodes) {
            if (node.getOwner() == newNode.getOwner()) {
                return false;
            }
        }

        nodes.add(newNode);

        return true;
    }

    List<InterestNode> getNodes() {
        return nodes;
    }

    void forget() {
        for (InterestNode node : nodes) {
            node.delete();
        }

        nodes.clear();
    }

}
