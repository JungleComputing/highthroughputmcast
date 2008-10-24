package mcast.ht.graph;

import java.util.ArrayList;
import java.util.List;

public class AllOtherPeersGenerator<V> implements PossiblePeersGenerator<V> {

	private List<V> vertices;
	
	public AllOtherPeersGenerator(List<V> vertices) {
		this.vertices = vertices;
	}
	
	public List<V> generatePossiblePeers(V vertex) {
		ArrayList<V> result = new ArrayList<V>(vertices);
		
		result.remove(vertex);
		
		return result;
	}
	
}
