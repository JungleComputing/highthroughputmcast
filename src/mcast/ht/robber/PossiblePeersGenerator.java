package mcast.ht.robber;

import java.util.List;

public interface PossiblePeersGenerator<Node> {

	public List<Node> generatePossiblePeers(Node me);
	
}
