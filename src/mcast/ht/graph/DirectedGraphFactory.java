package mcast.ht.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.RandomAccess;

import mcast.ht.util.Defense;

public class DirectedGraphFactory {

	public static<V> DirectedGraph<V> createCompleteGraph(Collection<V> vertices) {
		Defense.checkNotNull(vertices, "vertices");
		
		DirectedGraph<V> g = new DirectedGraph<V>();

		// add all vertices
		for (V vertex: vertices) {
			g.addVertex(vertex);
		}

		// add all edges
		for (V from: vertices) {
			for (V to: vertices) {
				if (!from.equals(to)) {
					g.addEdge(from, to);
				}
			}
		}

		return g;
	}

	public static<V> DirectedGraph<V> createMinDegreeRandomGraph(List<V> vertices, 
	        int minDegree, long seed, PossiblePeersGenerator<V> peerGenerator) {
		Defense.checkNotNull(vertices, "vertices");
		Defense.checkNotNegative(minDegree, "minimum degree");

		DirectedGraph<V> g = new DirectedGraph<V>();

		// add all vertices
		for (V vertex: vertices) {
			g.addVertex(vertex);
		}

		// dump collection in an array if the given list does not support 
		// random access
		List<V> raVertices = vertices;
		if (!(vertices instanceof RandomAccess)) {
			raVertices = new ArrayList<V>(vertices);
		}
		
		// add per vertex minDegree edges to randomly chosen peers
		Random r = new Random(seed);
		
		for (V vertex: raVertices) {
			List<V> possiblePeers = peerGenerator.generatePossiblePeers(vertex);
			
			// if possible_peers < minDegree, we cannot get minDegree peers
			int maxPeers = Math.min(minDegree, possiblePeers.size());
			
			for (int i = 0; i < maxPeers; i++) {
				
				int peerIndex = r.nextInt(possiblePeers.size());
				V peer = possiblePeers.remove(peerIndex);
				
				g.addEdge(vertex, peer);
				g.addEdge(peer, vertex);
			}
		}
		
		return g;
	}
	
}
