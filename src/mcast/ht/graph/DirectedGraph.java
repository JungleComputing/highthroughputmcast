package mcast.ht.graph;

import java.util.*;
import java.io.*;

import mcast.ht.util.Defense;

/**
 * This class implements a directed graph. Any object other than 
 * <code>null</code> can be used as a vertex. There can exist at most one edge 
 * from a vertex to another vertex. The edges of the graph can be annotated with 
 * any object, including <code>null</code>. Note that a directed graph is only 
 * Serializable when all vertex and edge annotated objects are also 
 * serializable.
 */
public class DirectedGraph<V> implements Serializable, Cloneable {

	private enum Direction { IN, OUT, ALL };

	/**
	 * Maps vertex objects to their corresponding Node objects that contain the
	 * lists of incoming and outgoing edges per vertex.
	 */
	protected LinkedHashMap<V, Node> nodes;

	/**
	 * Replacement data for unannotated edges.
	 */
	private static final Object DUMMY_DATA = new Object();

	private int noEdges;
	private int hashCode;

	/**
	 * Creates a new directed graph without any vertices or edges.
	 */
	public DirectedGraph() {
		nodes = new LinkedHashMap<V, Node>();
		hashCode = 0;
		noEdges = 0;
	}

	/**
	 * Copy constructor. The data the edges are annotated with and the vertices 
	 * are not copied.
	 */
	public DirectedGraph(DirectedGraph<V> original) {
		nodes = new LinkedHashMap<V, Node>(original.nodes.size(), 0.75f);

		// reuse the vertices and data, only create new nodes
		for (Iterator<V> i = original.vertices().iterator(); i.hasNext(); ) {
			V vertex = i.next();
			nodes.put(vertex, new Node(original.getNode(vertex)));
		}

		noEdges = original.noEdges;
		hashCode = original.hashCode;
	}

	/**
	 * Clears this graph so it contains no vertices or edges.
	 */
	public void clear() {
		nodes.clear();
	}

	/**
	 * Returns the node that contains the given vertex
	 *
	 * @return the node associated with the given identifier
	 */
	protected Node getNode(V vertex) {
		return (Node)(nodes.get(vertex));
	}

	/**
	 * Adds a new vertex to this graph. If the vertex already exists (as 
	 * determined by <code>equals()</code>) nothing happens.
	 *
	 * @param vertex the vertex to add
	 *
	 * @return <code>true</code> if the vertex was really added, 
	 * <code>false</code> if not.
	 *
	 * @throws NullPointerException if the specified vertex is 
	 * <code>null</code>.
	 */
	public boolean addVertex(V vertex) {
		checkNotNull(vertex, "vertex");

		if (getNode(vertex) == null) {
			nodes.put(vertex, new Node(vertex));
			hashCode += vertex.hashCode();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Adds all vertices and edges in the given graph to this graph. Existing 
	 * vertices and edges are left untouched, except of both the old and new 
	 * edge are annotated. In that case, the annotation of the old edge is 
	 * replaced with the new annotation.
	 */
	public void addGraph(DirectedGraph<V> g) {
		Defense.checkNotNull(g, "graph");
		
		for (V v: g.vertices()) {
			addVertex(v);
		}
		
		for (Edge<V> e: g.edges()) {
			addEdge(e.source, e.destination, e.data);
		}
	}

	/**
	 * Removes the specified vertex from this graph. All incoming and outgoing 
	 * edges of the vertex are also removed. If the vertex did not exist, 
	 * nothing happens.
	 *
	 * @param vertex the vertex to remove
	 *
	 * @return <code>true</code> if the vertex and all its incoming and outgoing 
	 * edges were really removed, <code>false</code> if not.
	 *
	 * @throws NullPointerException if the specified vertex is 
	 * <code>null</code>.
	 */
	public boolean removeVertex(V vertex) {
		checkNotNull(vertex, "vertex");

		Node node = getNode(vertex);

		if (node == null) {
			return false;
		} else {
			node.removeAllOutgoingEdges();
			node.removeAllIncomingEdges();
			nodes.remove(vertex);
			hashCode -= vertex.hashCode();
			return true;
		}
	}

	/**
	 * Returns whether this graph contains the specified vertex.
	 *
	 * @param vertex the vertex to check
	 *
	 * @return <code>true</code> if this graph contains the specified vertex,
	 *         <code>false</code> otherwise.
	 */
	public boolean containsVertex(V vertex) {
		return vertex != null && nodes.containsKey(vertex);
	}

	/**
	 * Adds an edge between the specified vertices to this graph, annotated 
	 * with the given data. If this graph does not contains one or more of the 
	 * specified vertices, these vertices are also added. If this graph already 
	 * contains an edge between the specified vertices, the data of the 
	 * existing edge is replaced with the specified data.
	 *
	 * @param source the source vertex of the edge
	 * @param destination the destination vertex of the edge
	 * @param data the data to annotate the edge with
	 *
	 * @return the data the edge between the specified vertices was previously 
	 * annotated with, or <code>null</code> if the edge did not already exist.
	 *
	 * @throws NullPointerException if one of the vertices is <code>null</code>.
	 */
	public Object addEdge(V source, V destination, Object data) {
		checkNotNull(source, "source");
		checkNotNull(destination, "destination");

		addVertex(source);
		addVertex(destination);

		Object internalData = createInternalData(data);
		Object storedData = getNode(source).addEdgeTo(destination, internalData);

		if (storedData == null) noEdges++;

		return createReturnData(storedData);
	}

	/**
	 * Substitutes a DUMMY_DATA object with null
	 */
	private Object createReturnData(Object data) {
		if (data == DUMMY_DATA) {
			return null;
		} else {
			return data;
		}
	}

	/**
	 * Substitutes null with a DUMMY_DATA object
	 */
	private Object createInternalData(Object data) {
		if (data == null) {
			return DUMMY_DATA;
		} else {
			return data;
		}
	}

	/**
	 * Utility function, identical to 
	 * <code>addEdge(source, destination, null)</code>.
	 */
	public Object addEdge(V source, V destination) {
		return addEdge(source, destination, null);
	}

	/**
	 * Utility function, identical to <code>addEdge(source, destination); 
	 * addEdge(destination, source)</code>. Existing edges are removed, 
	 * including any annotated data.  
	 */
	public void addTwoEdges(V source, V destination) {
		addEdge(source, destination, null);
		addEdge(destination, source, null);
	}

	/**
	 * Utility function, identical to 
	 * <code>addEdge(source, destination, data)</code>.
	 */
	public Object setDataBetween(V source, V destination, Object data) {
		return addEdge(source, destination, data);
	}

	/**
	 * Removes an edge between the specified vertices from this graph. If the
	 * edge does not exist, nothing happens.
	 *
	 * @param source the source vertex of the edge
	 * @param destination the destination vertex of the edge
	 *
	 * @return the data the removed edge was annotated with, or 
	 * <code>null</code> if the edge did not exist or was not annotated.
	 */
	public Object removeEdge(V source, V destination) {
		if (containsVertex(source) && containsVertex(destination)) {
			Object data = getNode(source).removeEdgeTo(destination);
			if (data != null) noEdges--;
			return createReturnData(data);
		} else {
			return null;
		}
	}

	/**
	 * Returns whether this graph contains an edge between the specified 
	 * vertices.
	 *
	 * @param source the source vertex of the edge
	 * @param destination the destination vertex of the edge
	 *
	 * @return <code>true</code> if this graph contains the specified edge,
	 *         <code>false</code> if not.
	 */
	public boolean containsEdge(V source, V destination) {
		Node node = getNode(source);
		return node != null && node.getDataTo(destination) != null;
	}

	/**
	 * Returns the data the edge between the specified vertices is annotated 
	 * with.
	 *
	 * @param source the source vertex of the edge
	 * @param destination the destination vertex of the edge
	 *
	 * @return the data the edge between the specified vertices is annotated 
	 * with, or <code>null</code> is the edge does not exist.
	 */
	public Object getData(V source, V destination) {
		if (containsVertex(source) && containsVertex(destination)) {
			return createReturnData(getNode(source).getDataTo(destination));
		} else {
			return null;
		}
	}

	/**
	 * Returns the number of vertices in this graph.
	 *
	 * @return the number of vertices in this graph.
	 */
	public int noVertices() {
		return nodes.size();
	}

	/**
	 * Returns the number of edges in this graph.
	 *
	 * @return the number of edges in this graph.
	 */
	public int noEdges() {
		return noEdges;
	}

	/**
	 * Returns an unmodifiable set containing the vertices of this graph.
	 * The iterator of the set throws a 
	 * <code>ConcurrentModificationException</code> when a vertex is added to 
	 * or removed from this graph while using the iterator.
	 *
	 * @return a set containing all vertices of this graph.
	 *
	 * @see ConcurrentModificationException
	 */
	public Set<V> vertices() {
		return Collections.unmodifiableSet(nodes.keySet());
	}

	/**
	 * Returns a set containing all the edges of this graph. Each element of
	 * the set is a <code>Edge</code>.
	 *
	 * @return a set containing all the edges of this graph.
	 *
	 * @see Edge
	 */
	public Set<Edge<V>> edges() {
		LinkedHashSet<Edge<V>> result = new LinkedHashSet<Edge<V>>(noEdges());

		for (V src: vertices()) {
		    for (V dst: outgoingNeighbors(src)) {
				Object data = createReturnData(getNode(src).getDataTo(dst));
				Edge<V> edge = new Edge<V>(src, dst, data);
				result.add(edge);
			}
		}

		return result;
	}

	/**
	 * Returns a string representation of this directed graph.
	 */
	public String toString() {
		String result = "";

		// add edges
		for (Iterator<Edge<V>> it = edges().iterator(); it.hasNext(); ) {
			Edge<V> edge = it.next();
			result += "# " + edge.source.toString() + " -> " +
				edge.destination.toString();
			if (edge.data != null) {
				result += ": " + edge.data.toString();
			}
			result += "\n";
		}

		// add unconnected vertices
		for (Iterator<V> it = vertices().iterator(); it.hasNext(); ) {
			V vertex = it.next();
			if (degree(vertex) == 0) {
				result += "# " + vertex.toString() + "\n";
			}
		}

		return result;
	}

	/**
	 * Returns an unmodifiable set of all the incoming neighbors of the 
	 * specified vertex. A vertex A is an incoming neighbor of vertex B if an 
	 * edge exists from A to B.
	 *
	 * @param vertex the vertex
	 *
	 * @return an unmodifiable set of all the incoming neighbors of the 
	 * specified vertex.
	 *
	 * @throws NoSuchElementException if this graph does not contain the 
	 * specified vertex.
	 */
	public Set<V> incomingNeighbors(V vertex) {
		checkVertexContained(vertex);

		return new HashSet<V>(getNode(vertex).incomingNeighbors());
	}

	/**
	 * Returns an unmodifiable set of all the outgoing neighbors of the 
	 * specified vertex. A vertex A is an outgoing neighbor of vertex B if an 
	 * edge exists from B to A.
	 *
	 * @param vertex the vertex
	 *
	 * @return an unmodifiable set of all the outgoing neighbors of the 
	 * specified vertex.
	 *
	 * @throws NoSuchElementException if this graph does not contain the 
	 * specified vertex.
	 */
	public Set<V> outgoingNeighbors(V vertex) {
		checkVertexContained(vertex);

		return new HashSet<V>(getNode(vertex).outgoingNeighbors());
	}

	/**
	 * Returns a set of all incoming and outgoing neighbors of the specified 
	 * vertex. Note that when a vertex in both an incoming and an outgoing 
	 * neighbor, it is included only once. Modifying the returned set does not 
	 * affect the structure of this directed graph.
	 *
	 * @param vertex the vertex
	 *
	 * @return a set of all the incoming and outgoing neighbors of the 
	 * specified vertex.
	 *
	 * @throws NoSuchElementException if this graph does not contain the 
	 * specified vertex.
	 */
	public Set<V> neighbors(V vertex) {
		checkVertexContained(vertex);

		return getNode(vertex).neighbors();
	}

	/**
	 * Removes all incoming edges at the specified vertex.
	 *
	 * @param vertex the vertex
	 *
	 * @throws NoSuchElementException if this graph does not contain the 
	 * specified vertex.
	 */
	public void removeAllIncomingEdges(V vertex) {
		checkVertexContained(vertex);

		noEdges -= getNode(vertex).removeAllIncomingEdges();
	}

	/**
	 * Removes all outgoing edges from the specified vertex.
	 *
	 * @param vertex the vertex
	 *
	 * @throws NoSuchElementException if this graph does not contain the 
	 * specified vertex.
	 */
	public void removeAllOutgoingEdges(V vertex) {
		checkVertexContained(vertex);

		noEdges -= getNode(vertex).removeAllOutgoingEdges();
	}

	/**
	 * Removes all edges incident on the specified vertex.
	 *
	 * @param vertex the vertex
	 *
	 * @throws NoSuchElementException if this graph does not contain the 
	 * specified vertex.
	 */
	public void removeAllEdges(V vertex) {
		checkVertexContained(vertex);

		Node node = getNode(vertex);
		node.removeAllIncomingEdges();
		node.removeAllOutgoingEdges();
	}

	/**
	 * Returns the number of incoming neighbors of the specified vertex.
	 * A vertex A is an incoming neighbor of vertex B if an edge from A to B 
	 * exists.
	 *
	 * @param vertex the vertex
	 *
	 * @return the number of incoming neighbors
	 *
	 * @throws NoSuchElementException if this graph does not contain the 
	 * specified vertex.
	 */
	public int inDegree(V vertex) {
		checkVertexContained(vertex);

		return getNode(vertex).inDegree();
	}

	/**
	 * Returns the number of outgoing neighbors of the specified vertex.
	 * A vertex A is an outgoing neighbor of vertex B if an edge from B to A 
	 * exists.
	 *
	 * @param vertex the vertex
	 *
	 * @return the number of outgoing neighbors
	 *
	 * @throws NoSuchElementException if this graph does not contain the 
	 * specified vertex.
	 */
	public int outDegree(V vertex) {
		checkVertexContained(vertex);

		return getNode(vertex).outDegree();
	}

	private void checkNotNull(V vertex, String name) {
		if (vertex == null) {
			throw new NullPointerException(name + " cannot be null");
		}
	}

	private void checkVertexContained(V vertex) {
		if (!nodes.containsKey(vertex)) {
			throw new NoSuchElementException("unknown vertex: " + vertex);
		}
	}

	/**
	 * Returns the degree of a vertex. This method essentially performs
	 * <code>return inDegree(vertex) + outDegree(vertex)</code>.
	 *
	 * @param vertex the vertex
	 *
	 * @return the degree of a vertex
	 */
	public int degree(V vertex) {
		checkVertexContained(vertex);

		Node node = getNode(vertex);
		return node.inDegree() + node.outDegree();
	}

	/**
	 * Returns a shallow copy of this directed graph. All the structure of the 
	 * graph itself is copied, but the vertices and data the edges are 
	 * annotated with are not cloned.
	 */
	public Object clone() {
		return new DirectedGraph<V>(this);
	}

	public boolean isWeaklyConnected() {
		if (noVertices() <= 1) {
			return true;
		} else {
			HashSet<V> connected = new HashSet<V>(noVertices());
			V start = vertices().iterator().next();
			
			searchNeighborsBreadthFirst(start, connected, Direction.ALL);
			
			return connected.size() == noVertices();
		}
	}

	public boolean isStronglyConnectedSlow() {
		if (noVertices() <= 1) {
			return true;
		} else {
			HashSet<V> connectedVertices = new HashSet<V>(noVertices());
			
			for(V v: vertices()) {
			
				searchNeighborsBreadthFirst(v, connectedVertices, Direction.OUT);
				
				if (connectedVertices.size() < noVertices()) {
					return false;
				}
				
				connectedVertices.clear();
			}

			return true;
		}
	}
	
	public boolean isStronglyConnected() {
		if (noVertices() <= 1) {
			return true;
		} else {
			V start = vertices().iterator().next();
			Set<Set<V>> components = getStronglyConnectedComponents(start, 1);
			Set<V> component = components.iterator().next();
			return component.size() == noVertices();
		}
	}
	
	/**
	 * Returns all strongly connected components in this graph that can be 
	 * reached from the given vertex.
	 */
	public Set<Set<V>> getStronglyConnectedComponents(V vertex) {
		return getStronglyConnectedComponents(vertex, noVertices());
	}

	/* This method uses Tarjan's algorithm to find the strongly connected
	 * components in this graph. Pseudo-code:
	 * <pre>
	 * Input: Graph G = (V, E), Start node v0
	 * 
	 * index = 0                       // DFS node number counter 
	 * S = empty                       // An empty stack of nodes
	 * tarjan(v0)                      // Start a DFS at the start node
	 *
	 * procedure tarjan(v)
	 *   v.index = index               // Set the depth index for v
  	 *   v.lowlink = index
  	 *   index = index + 1
  	 *   S.push(v)                     // Push v on the stack
  	 *   forall (v, v') in E do        // Consider successors of v 
     *     if (v'.index is undefined)  // Was successor v' visited? 
     *       tarjan(v')                // Recurse
     *       v.lowlink = min(v.lowlink, v'.lowlink)
     *     elseif (v' in S)            // Is v' on the stack?
     *       v.lowlink = min(v.lowlink, v'.index)
  	 *   if (v.lowlink == v.index)     // Is v the root of an SCC?
     *     print "SCC:"
     *     repeat
     *       v' = S.pop
     *       print v'
     *     until (v' == v)
     * </pre>
     * The search is stopped when <max> components are found
	 */
	private Set<Set<V>> getStronglyConnectedComponents(V start, int max) {
		Set<Set<V>> components = new HashSet<Set<V>>();
		
		if (noVertices() > 0 && max > 0) {
			LinkedHashMap<V, TarjanNode> tarjanNodes = 
			        new LinkedHashMap<V, TarjanNode>();
			LinkedList<V> stack = new LinkedList<V>();
			tarjan(components, max, 0, stack, tarjanNodes, start);
		}

		return components;
	}
	
	private int tarjan(Set<Set<V>> components, int max, int index, 
	        LinkedList<V> stack, 
	        LinkedHashMap<V, TarjanNode> tarjanNodes, V vertex) {
		
	    TarjanNode node = getTarjanNode(tarjanNodes, vertex);
		
		node.index = index;
		node.lowlink = index;
		
		index = index + 1;
		
		stack.add(vertex);
		node.onStack = true;
		
		for(V neighbor: outgoingNeighbors(vertex)) {
			TarjanNode neighborNode = getTarjanNode(tarjanNodes, neighbor);
			
			if (neighborNode.index < 0) {
				index = tarjan(components, max, index, stack, tarjanNodes, 
				            neighbor);
				
				if (components.size() == max) {
					return index;
				} else {
					node.lowlink = Math.min(node.lowlink, neighborNode.lowlink);
				}
			} else if (neighborNode.onStack) {
				node.lowlink = Math.min(node.lowlink, neighborNode.index);
			}
		}
		
		if (node.lowlink == node.index) {
			Set<V> component = new HashSet<V>(); 
			V member = null;
			
			do {
				member = stack.removeLast();
				TarjanNode memberNode = getTarjanNode(tarjanNodes, member);
				memberNode.onStack = false;
				component.add(member);
			} while (!member.equals(vertex));
			
			components.add(component);
		}
		
		return index;
	}
	
	private TarjanNode getTarjanNode(LinkedHashMap<V, TarjanNode> tarjanNodes, 
	        V vertex) {
		TarjanNode result = tarjanNodes.get(vertex);
		
		if (result == null) {
			result = new TarjanNode();
			tarjanNodes.put(vertex, result);
		}
		
		return result;
	}
	
	private static class TarjanNode {
		int index;
		int lowlink;
		boolean onStack;
		
		TarjanNode() {
			index = -1;
			lowlink = -1;
			onStack = false;
		}
	}
	
	private void searchNeighborsDepthFirst(V vertex, HashSet<V> verticesFound, 
	        Direction direction) {
		for (V neighbor: getNeighbors(vertex, direction)) {
			if (verticesFound.add(neighbor) && 
			    verticesFound.size() < noVertices()) {
			    
				searchNeighborsDepthFirst(neighbor, verticesFound, direction);
			}
		}
	}

	private void searchNeighborsBreadthFirst(V root, HashSet<V> verticesFound, 
	        Direction direction) {
		LinkedList<V> todo = new LinkedList<V>();
		todo.addFirst(root);

		while(!todo.isEmpty()) {
			V vertex = todo.removeFirst();
			
			for (V child: getNeighbors(vertex, direction)) {
				if (verticesFound.add(child)) {
					todo.addLast(child);
				}
			}
		}
	}

	/**
	 * Returns a graph that contains all vertices of this graph that are part of
	 * the specified set. All edges between those vertices in this graph
	 * are also part of the subgraph.
	 *
	 * @param vertices the set of vertices that indicate which vertices
	 *                 of this graph should be part of the subgraph.
	 *
	 * @return a graph that contains all vertices of this graph that are part of
	 * the specified set, and all edges between those vertices that exist in 
	 * this graph. The vertices and edge data itself are not copied.
	 */
	public DirectedGraph<V> subgraph(Set<V> vertices) {
		DirectedGraph<V> result = new DirectedGraph<V>();

		for (Iterator<V> it = vertices.iterator(); it.hasNext(); ) {
			V vertex = it.next();

			if (containsVertex(vertex)) {
				result.addVertex(vertex);
			}

			for (V neighbor: outgoingNeighbors(vertex)) {
				if (vertices.contains(neighbor)) {
					result.addEdge(vertex, neighbor, getData(vertex, neighbor));
				}
			}
		}
		return result;
	}

	/**
	 * Returns the directed rooted tree that is the result of a depth first 
	 * search from a specified root node.
	 *
	 * @param root the node in this graph where the depth first search must 
	 * start
	 *
	 * @return a directed tree rooted in the specified root node.
	 *
	 * @throws NoSuchElementException if the specified root node is not part of 
	 * this graph.
	 */
	public DirectedRootedTree<V> depthFirstSearch(V root) 
	        throws NoSuchElementException {
		checkNotNull(root, "root");
		checkVertexContained(root);

		DirectedRootedTree<V> result = new DirectedRootedTree<V>(root);
		doDepthFirstSearch(result, root, null, Direction.OUT);
		return result;
	}

	/**
	 * Returns the directed rooted tree that is the result of a depth first 
	 * search from a specified root node. The search goes against the direction 
	 * of the edges.
	 *
	 * @param root the node in this graph where the depth first search must 
	 * start
	 *
	 * @return a directed tree rooted in the specified root node.
	 *
	 * @throws NoSuchElementException if the specified root node is not part of 
	 * this graph.
	 */
	public DirectedRootedTree<V> depthFirstSearchReverseDirection(V root) 
	        throws NoSuchElementException {
		checkNotNull(root, "root");
		checkVertexContained(root);

		DirectedRootedTree<V> result = new DirectedRootedTree<V>(root);
		doDepthFirstSearch(result, root, null, Direction.IN);

		return result;
	}

	/**
	 * Returns the directed rooted tree that is the result of a depth first 
	 * search from a specified root node. The direction of the edges in this 
	 * graph are ignored during the seach.
	 *
	 * @param root the node in this graph where the depth first search must 
	 * start
	 *
	 * @return a directed tree rooted in the specified root node.
	 *
	 * @throws NoSuchElementException if the specified root node is not part of 
	 * this graph.
	 */
	public DirectedRootedTree<V> depthFirstSearchIgnoreDirection(V root) 
	        throws NoSuchElementException {
		checkNotNull(root, "root");
		checkVertexContained(root);

		DirectedRootedTree<V> result = new DirectedRootedTree<V>(root);
		doDepthFirstSearch(result, root, null, Direction.ALL);

		return result;
	}

	private boolean doDepthFirstSearch(DirectedRootedTree<V> result, V root, 
	        V target, Direction direction) {
		
	    for (Iterator<V> it = getNeighbors(root, direction).iterator(); 
		        it.hasNext() && !result.containsVertex(target); ) {
		    
			V neighbor = it.next();

			if (!result.containsVertex(neighbor)) {
				result.addChild(root, neighbor, getData(root, neighbor));
		
				if ((target != null && neighbor.equals(target)) ||               
					result.noVertices() == noVertices() ||                    
					doDepthFirstSearch(result, neighbor, target, direction)) {
				    // we found the target node, or all nodes are already 
				    // visited, or we did recursion
					return true;
				}
			}
		}
		return false;
	}

	private Set<V> getNeighbors(V vertex, Direction direction) {
		switch(direction) {
			case IN: return getNode(vertex).incomingNeighbors();
			case OUT: return getNode(vertex).outgoingNeighbors();
			case ALL: return getNode(vertex).neighbors();
			default:
				throw new RuntimeException("unknown direction: " + direction);
		}
	}

	public DirectedRootedTree<V> breadthFirstSearch(V root) 
	        throws NoSuchElementException {
		return doBreadthFirstSearch(root, null, Direction.OUT);
	}

	public DirectedRootedTree<V> breadthFirstSearch(V root, V target) 
	        throws NoSuchElementException {
		return doBreadthFirstSearch(root, target, Direction.OUT);
	}

	public DirectedRootedTree<V> doBreadthFirstSearch(V root, V target, 
	        Direction direction) throws NoSuchElementException {
		checkNotNull(root, "root");
		checkVertexContained(root);

		DirectedRootedTree<V> result = new DirectedRootedTree<V>(root);
		LinkedList<V> todo = new LinkedList<V>();
		todo.addFirst(root);

		while(!todo.isEmpty()) {
			V vertex = todo.removeFirst();
			
			for (V child: getNeighbors(vertex, direction)) {
				if (!result.containsVertex(child)) {
					result.addChild(vertex, child);
					
					if (target != null && child.equals(target)) {
					    return result;
					}
					
					todo.addLast(child);
				}
			}
		}

		return result;
	}

	public DirectedRootedTree<V> breadthFirstSearchReverseDirection(V root) 
	        throws NoSuchElementException {
		return breadthFirstSearchReverseDirection(root, null);
	}

	public DirectedRootedTree<V> breadthFirstSearchReverseDirection(V root, 
	        V target) throws NoSuchElementException {
		checkNotNull(root, "root");
		checkVertexContained(root);

		DirectedRootedTree<V> result = new DirectedRootedTree<V>(root);
		LinkedList<V> todo = new LinkedList<V>();
		todo.addFirst(root);

		while(!todo.isEmpty() && !result.containsVertex(target)) {
			V vertex = todo.removeFirst();

			for (V child: incomingNeighbors(vertex)) {
			    if (!result.containsVertex(child)) {
					result.addChild(vertex, child);
					todo.addLast(child);
				}
			}
		}

		return result;
	}

	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (other == null) {
			return false; 
		} else if (this.getClass() != other.getClass()) {
			return false; 
		}
	    
		DirectedGraph<?> rhs = (DirectedGraph<?>)other;
			
		if (noVertices() != rhs.noVertices() || noEdges() != rhs.noEdges()) {
			return false;
		} else {
			return edges().equals(rhs.edges());
		}
	}

	public int hashCode() {
		return hashCode;
	}

	// INNER CLASSES

	/**
	 * This class implements a node in a directed graph. A node contains a 
	 * vertex, zero or more incoming and zero or more outgoing edges.
	 * There can exists at most one edge to any other node.
	 */
	protected class Node implements Serializable {

		protected V vertex;
		protected LinkedHashMap<V, Object> in, out;

		Node(V vertex) {
			this.vertex = vertex;
			in = new LinkedHashMap<V, Object>();
			out = new LinkedHashMap<V, Object>();
		}

		@SuppressWarnings("unchecked")
		Node(Node original) {
			this.vertex = original.vertex;

			in = (LinkedHashMap<V, Object>)original.in.clone();
			out = (LinkedHashMap<V, Object>)original.out.clone();
		}

		V getVertex() {
			return vertex;
		}

		/**
		 * Adds an edge from this vertex to the specified destination vertex
		 * annotated with the specified data. If the edge already existed 
		 * (according to its <code>equals()</code> method), the existing data 
		 * is overwritten.
		 *
		 * @param destination the destination vertex of the edge to add
		 * @param data the data to annotate the edge with
		 *
		 * @return the previous data the edge to the specified vertex was 
		 * annotated with, or <code>null</code> if no such edge existed.
		 */
		Object addEdgeTo(V destination, Object data) {
			out.put(destination, data);
			
			hashCode += vertex.hashCode() + (2 * destination.hashCode()) + 
			        data.hashCode();
			
			return getNode(destination).in.put(vertex, data);
		}

		/**
		 * Adds an edge from the specified source vertex to this vertex
		 * annotated with the specified data. If the edge already existed 
		 * (according to its <code>equals()</COD> method), the existing data is 
		 * overwritten.
		 *
		 * @param source the source vertex of the edge to add
		 * @param data the data to annotate the edge with
		 *
		 * @return the previous data the edge to the specified vertex was 
		 * annotated with, or <code>null</code> if no such edge existed.
		 */
		Object addEdgeFrom(V source, Object data) {
			in.put(source, data);
			
			hashCode += source.hashCode() + (2 * vertex.hashCode()) + 
			        data.hashCode();
			
			return getNode(source).out.put(vertex, data);
		}

		/**
		 * Removes the edge from this vertex to the specified destination 
		 * vertex. If no such edge exists, nothing happens.
		 *
		 * @param destination the destination vertex
		 *
		 * @return the data the removed edge was annotated with,
		 *         or <code>null</code> if no such edge existed.
		 */
		Object removeEdgeTo(V destination) {
			Object data = out.remove(destination);
			if (data != null) {
				hashCode -= vertex.hashCode() + (2 * destination.hashCode()) + 
				        data.hashCode();
				return getNode(destination).in.remove(vertex);
			} else {
				return null;
			}
		}

		/**
		 * Removes the edge from the specified source vertex to this vertex.
		 * If no such edge exists, nothing happens.
		 *
		 * @param source the source vertex
		 *
		 * @return the data the removed edge was annotated with,
		 *         or <code>null</code> if no such edge existed.
		 */
		Object removeEdgeFrom(V source) {
			Object data = in.remove(source);
			if (data != null) {
				hashCode -= source.hashCode() + (2 * vertex.hashCode()) + 
				        data.hashCode();
				return getNode(source).out.remove(vertex);
			} else {
				return null;
			}
		}

		/**
		 * Reverses the direction of the edge to the specified destination 
		 * vertex (i.e. the source and destination of that vertex are swapped). 
		 * If no such edge exists, nothing happens.
		 *
		 * @param destination the destination vertex
		 *
		 * @return <code>false</code> if the edge did not exist, 
		 * <code>true</code> otherwise.
		 */
		boolean reverseEdgeTo(V destination) {
			if (out.containsKey(destination)) {
				in.put(destination, out.remove(destination));
				Node dstNode = getNode(destination);
				dstNode.out.put(vertex, dstNode.in.remove(vertex));
				hashCode = hashCode + vertex.hashCode() - destination.hashCode();
				return true;
			} else {
				return false;
			}
		}

		/**
		 * Reverses the direction of the edge from the specified source vertex. 
		 * (i.e. the source and destination of that vertex are swapped). If no 
		 * such edge exists, nothing happens.
		 *
		 * @param source the source vertex
		 *
		 * @return <code>false</code> if the edge did not exist, 
		 * <code>true</code> otherwise.
		 */
		boolean reverseEdgeFrom(V source) {
			if (in.containsKey(source)) {
				out.put(source, in.remove(source));
				Node sourceNode = getNode(source);
				sourceNode.in.put(vertex, sourceNode.out.remove(vertex));
				hashCode = hashCode + source.hashCode() - vertex.hashCode();
				return true;
			} else {
				return false;
			}
		}

		/**
		 * Returns whether an edge from this vertex to the specified destination 
		 * vertex exists.
		 *
		 * @param destination the destination vertex.
		 *
		 * @return <code>true</code> if an edge from this vertex to the 
		 * specified destination vertex exists, <code>false</code> otherwise.
		 */
		boolean containsEdgeTo(V destination) {
			return out.containsKey(destination);
		}

		/**
		 * Returns whether an edge from the specified source vertex to this 
		 * vertex exists.
		 *
		 * @param source the source vertex
		 *
		 * @return <code>true</code> if an edge from the specified source 
		 * vertex to this vertex exists, <code>false</code> otherwise.
		 */
		boolean containsEdgesFrom(V source) {
			return in.containsKey(source);
		}

		/**
		 * Returns the data of the edge from this vertex to the specified 
		 * destination vertex.
		 *
		 * @param destination the destination vertex
		 *
		 * @return the data of the edge from this vertex to the specified 
		 * destination vertex, or <code>null</code> if the edge did not exist.
		 */
		Object getDataTo(V destination) {
			return out.get(destination);
		}

		/**
		 * Returns the data of the edge from the specified source vertex to 
		 * this vertex.
		 *
		 * @param source the source vertex
		 *
		 * @return the data of the edge from the specified source vertex to 
		 * this vertex, or <code>null</code> if the edge did not exist.
		 */
		Object getDataFrom(V source) {
			return in.get(source);
		}

		/**
		 * Removes all outgoing edges of this vertex.
		 *
		 * @return the number of outgoing edges this vertex had
		 */
		final int removeAllOutgoingEdges() {
			// remove each outgoing edge from the inset of its destination vertex
			for (Iterator<V> it = out.keySet().iterator(); it.hasNext(); ) {
				Node node = getNode(it.next());
				Object data = node.in.remove(vertex);
				hashCode -= vertex.hashCode() + (2 * node.vertex.hashCode()) + 
				        data.hashCode();
			}

			// clear the outset of this vertex
			int noEdges = out.size();
			out.clear();
			return noEdges;
		}

		/**
		 * Removes all incoming edges of this vertex.
		 *
		 * @return the number of incoming edges this vertex had
		 */
		final int removeAllIncomingEdges() {
			// remove each incoming edge from the outset of its source vertex
			for (Iterator<V> it = in.keySet().iterator(); it.hasNext(); ) {
				Node node = getNode(it.next());
				Object data = node.out.remove(vertex);
				hashCode -= (2 * vertex.hashCode()) + node.vertex.hashCode() + 
				        data.hashCode();
			}

			// clear the inset of this vertex
			int noEdges = in.size();
			in.clear();
			return noEdges;
		}

		/**
		 * Returns the number of incoming edges of this vertex.
		 *
		 * @return the number of incoming edges of this vertex.
		 */
		int inDegree() {
			return in.size();
		}

		/**
		 * Returns the number of outgoing edges of this vertex.
		 *
		 * @return the number of outgoing edges of this vertex.
		 */
		int outDegree() {
			return out.size();
		}

		/**
		 * Returns a set of all source vertices of incoming edges
		 * of this vertex.
		 *
		 * @return the set.
		 */
		Set<V> incomingNeighbors() {
			return in.keySet();
		}

		/**
		 * Returns an iterator over all destination vertices of outgoing edges
		 * of this vertex.
		 *
		 * @return the set.
		 */
		Set<V> outgoingNeighbors() {
			return out.keySet();
		}

		/**
		 * Returns an unmodifiable set of all neighbor vertices of this vertex.
		 * Vertices that are both an incoming and outgoing neighbor will be 
		 * returned only once.
		 *
		 * @return the set.
		 */
		Set<V> neighbors() {
			LinkedHashSet<V> result = new LinkedHashSet<V>(in.keySet());
			result.addAll(out.keySet());
			return result;
		}

		/**
		 * Compares this node with the specified object for equality. Returns 
		 * true if the specified object is also a Node and its vertex is equal 
		 * to the vertex of this node.
		 *
		 * @param o the object to compare this node with for equality
		 *
		 * @return <code>true</code> if this node is equal to the specified 
		 * object.
		 */
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else {
				Node rhs = (Node)o;
				return vertex.equals(rhs.vertex);
			}
		}

		public int hashCode() {
			return vertex.hashCode();
		}

		public Object clone() {
			return new Node(this);
		}

	}

}
