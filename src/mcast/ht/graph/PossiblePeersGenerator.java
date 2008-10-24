package mcast.ht.graph;

import java.util.List;

public interface PossiblePeersGenerator<V> {

	public List<V> generatePossiblePeers(V me);
	
}
