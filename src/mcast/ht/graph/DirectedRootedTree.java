package mcast.ht.graph;

import java.util.*;
import java.io.*;

/**
 * This class implements a directed, rooted tree.
 * Any object other than <code>null</code> can be used as a vertex.
 * The edges of the tree can be annotated with any object.
 * By default, they are annotated with <code>null</code>.
 */
public class DirectedRootedTree<V> implements Serializable {

	protected DirectedGraph<V> digraph;
	protected V root;

	/**
	 * Creates a new directed tree with one root vertex
	 */
	public DirectedRootedTree(V root) {
		this.root = root;
		digraph = new DirectedGraph<V>();
		digraph.addVertex(root);
	}

	/**
	 * Copy constructor. Only the structure of the tree is copied,
	 * not the vertices or annotation objects.
	 */
	public DirectedRootedTree(DirectedRootedTree<V> original) {
		digraph = new DirectedGraph<V>(original.digraph);
		root = original.root;
	}

	/**
	 * Adds an unannotated edge to the tree. If this tree already
	 * contains the edge, the object it is annotated with is removed.
	 *
	 * @param parent the source vertex of the edge
	 * @param child the destination vertex of the edge
	 *
	 * @return the object the edge was previously annotated with,
	 *         or <code>null</code> if the edge did not exist yet.
	 *
	 * @throws IllegalArgumentException if one of the vertices is 
	 * <code>null</code>.
	 * @throws NoSuchElementException if the parent vertex is not already part 
	 * of this tree.
	 */
	public Object addChild(V parent, V child) throws IllegalArgumentException, 
	        NoSuchElementException {
		return addChild(parent, child, null);
	}

	/**
	 * Adds an annotated edge to the tree. If this tree already
	 * contains the edge, the object it is annotated with is removed.
	 *
	 * @param parent the source vertex of the edge
	 * @param child the destination vertex of the edge
	 * @param data the data the edge is annotated with
	 *
	 * @return the object the edge was previously annotated with,
	 *         or <code>null</code> if the edge did not exist yet.
	 *
	 * @throws NullPointerException if one of the vertices is <code>null</code>.
	 * @throws NoSuchElementException if the parent vertex is not already part 
	 * of this tree.
	 */
	public Object addChild(V parent, V child, Object data) 
	        throws IllegalArgumentException, NullPointerException {
		if (digraph.containsVertex(parent)) {
			return digraph.addEdge(parent, child, data);
		} else {
			throw new NoSuchElementException("unknown vertex: " + parent);
		}
	}

	/**
	 * Connects another tree to a vertex in this graph by adding an
	 * edge from a vertex in this graph to the root of the subtree.
	 * The structure of the subtree is copied, its vertices and data
	 * its edges are annotated with is not.
	 *
	 * @param parent the vertex in this graph the subtree should be connected to
	 * @param subtree the directed rooted tree that should be connected to this 
	 * tree
	 *
	 * @throws NoSuchElementException if the parent vertex is not already part 
	 * of this tree.
	 */
	public void addSubtree(V parent, DirectedRootedTree<V> subtree) 
	        throws NoSuchElementException {
		addChild(parent, subtree.root());
		subtree.attachAllChildren(this, subtree.root());
	}

	/**
	 * Removes an edge and the subtree below it from this tree. If the edge
	 * does not exist, nothing happens.
	 *
	 * @param parent the source vertex of the edge to remove
	 * @param child the destination vertex of the edge to remove
	 */
	public void removeChild(V parent, V child) {
		if (containsVertex(parent) && containsVertex(child)) {
			doRemoveChild(parent, child);
		}
	}

	private void doRemoveChild(V parent, V child) {
		for (V grandchild: children(child)) {
			doRemoveChild(child, grandchild);
		}
		digraph.removeEdge(parent, child);
		digraph.removeVertex(child);
	}

	/**
	 * Returns the set of all children of a vertex in this tree.
	 *
	 * @param parent the parent vertex
	 *
	 * @return the set of all children of the specified parent vertex in this 
	 * tree.
	 *
	 * @throws NoSuchElementException if the vertex is not part of this tree.
	 */
	public Set<V> children(V parent) throws NoSuchElementException {
		return digraph.outgoingNeighbors(parent);
	}

	/**
	 * Returns the number of children of a vertex in this tree.
	 *
	 * @return the number of children of a vertex in this tree.
	 *
	 * @throws NoSuchElementException if the vertex is not part of this tree.
	 */
	public int noChildren(V vertex) throws NoSuchElementException {
		return digraph.outDegree(vertex);
	}

	/**
	 * Returns the parent vertex of the specified vertex, or <code>null</code>
	 * if the specified vertex is the root vertex of this tree.
	 *
	 * @param vertex a vertex in this tree
	 *
	 * @return the parent vertex of the specified vertex, or <code>null</code>
	 *         if the specified vertex is the root vertex of this tree.
	 *
	 * @throws NoSuchElementException if the vertex is not part of this tree.
	 */
	public V parent(V vertex) throws NoSuchElementException {
		if (vertex.equals(root)) {
			return null;
		} else {
			return digraph.incomingNeighbors(vertex).iterator().next();
		}
	}

	/**
	 * Returns the subtree below the specified vertex, including that vertex 
	 * itself. The structure of the resulting tree is a copy of this tree, the 
	 * vertices and data the edges are annotated with are not copied.
	 *
	 * @param root a vertex in this tree
	 *
	 * @return the subtree in the tree below the specified vertex.
	 *
	 * @throws NoSuchElementException if the specified root is not part of this 
	 * tree.
	 */
	public DirectedRootedTree<V> getSubtree(V root) 
	        throws NoSuchElementException {
		return digraph.depthFirstSearch(root);
	}

	/**
	 * Returns whether <i>descendant</i> is a descendant of <i>vertex</i> in 
	 * this tree. This is the case of <i>descendant</i> is part of the subtree 
	 * of <i>vertex</i>.
	 *
	 * @param vertex the elder vertex
	 * @param descendant the vertex that could be a descendant of <i>vertex</i>
	 *
	 * @return <code>true</code> is <i>descendant</i> is a descendant of 
	 * <i>vertex</i>, <code>false</code> otherwise.
	 *
	 * @throws NoSuchElementException if <vertex> is not part of this tree
	 */
	public boolean isDescendant(V vertex, V descendant) {
		DirectedRootedTree<V> subtree = 
		        digraph.breadthFirstSearch(vertex, descendant);
		return subtree.containsVertex(descendant);
	}

	/**
	 * Returns the data the edge between the specified vertices is annotated 
	 * with.
	 *
	 * @param parent the source vertex of the edge
	 * @param child the destination vertex of the edge
	 *
	 * @return the data the edge between the specified vertices is annotated 
	 * with, or <code>null</code> is the edge does not exist. Note that by 
	 * default, edges are annotated with <code>null</code>, so a return value of
	 * <code>null</code> does not necessarily mean that the edge does not exist.
	 */
	public Object getDataBetween(V parent, V child) {
		return digraph.getData(parent, child);
	}

	private void attachAllChildren(DirectedRootedTree<V> tree, V parent) 
	        throws NoSuchElementException {
		for (V child: children(parent)) {
		    tree.addChild(parent, child, digraph.getData(parent, child));
			attachAllChildren(tree, child);
		}
	}

	/**
	 * Returns whether this tree contains the specified vertex.
	 *
	 * @param vertex the vertex to check
	 *
	 * @return <code>true</code> if this tree contains the specified vertex,
	 *         <code>false</code> otherwise.
	 */
	public boolean containsVertex(V vertex) {
		return digraph.containsVertex(vertex);
	}

	/**
	 * Returns whether this tree contains an certain edge.
	 *
	 * @param parent the source vertex of the edge
	 * @param child the destination vertex of the edge
	 *
	 * @return <code>true</code> if this tree contains an edge from <parent> 
	 * to <child>, <code>false</code> otherwise.
	 */
	public boolean containsEdge(V parent, V child) {
		return digraph.containsEdge(parent, child);
	}

	/**
	 * Returns an unmodifiable set containing the vertices of this tree.
	 * The iterator of the set throws a 
	 * <code>ConcurrentModificationException</code> when a vertex is added to 
	 * or removed from this graph while using the iterator.
	 *
	 * @return a set containing all vertices of this tree.
	 *
	 * @see ConcurrentModificationException
	 */
	public Set<V> vertices() {
		return digraph.vertices();
	}


	/**
	 * Returns the number of vertices in this tree.
	 *
	 * @return the number of vertices in this tree.
	 */
	public int noVertices() {
		return digraph.noVertices();
	}

	/**
	 * Returns the root vertex of this tree
	 *
	 * @return the root vertex of this tree
	 */
	public V root() {
		return root;
	}

	/**
	 * Returns a set containing all the edges of this tree. Each element of
	 * the set is a <code>Edge</code>.
	 *
	 * @return a set containing all the edges of this tree.
	 *
	 * @see DirectedGraph.Edge
	 */
	public Set<Edge<V>> edges() {
		return digraph.edges();
	}

	/**
	 * Returns a list containing all the edges of this tree, ordered as when 
	 * the tree is traversed by depth-first search starting at the root.
	 * Each element of the list is a <code>DirectedGraph.Edge</code>.
	 *
	 * @return a list containing all the edges of this tree.
	 *
	 * @see Edge
	 */
	public List<Edge<V>> edgesDepthFirstSearch() {
		ArrayList<Edge<V>> result = new ArrayList<Edge<V>>(noEdges());
		addEdgesDepthFirstSearch(result, root);
		return result;
	}

	private void addEdgesDepthFirstSearch(ArrayList<Edge<V>> list, V pivot) {
		for (V child: children(pivot)) {
			Edge<V> edge = 
			        new Edge<V>(pivot, child, digraph.getData(pivot, child));
			list.add(edge);

			addEdgesDepthFirstSearch(list, child);
		}
	}

	/**
	 * Returns the number of edges in this tree.
	 *
	 * @return the number of edges in this tree.
	 */
	public int noEdges() {
		return noVertices() - 1;
	}

	/**
	 * Returns whether a vertex is a leaf vertex in this tree (that is: a vertex
	 * without any children).
	 *
	 * @param vertex the vertex to check
	 *
	 * @return <code>true</code> if the vertex is a leaf, <code>false</code> 
	 * if not.
	 *
	 * @throws NoSuchElementException when the vertex is not part of this tree.
	 */
	public boolean isLeaf(V vertex) throws NoSuchElementException {
		if (digraph.containsVertex(vertex)) {
			return digraph.outDegree(vertex) == 0;
		} else {
			throw new NoSuchElementException("unknown vertex: " + vertex);
		}
	}

	/**
	 * Returns a directed graph representation of this tree. Changes to this 
	 * graph 'write-through' to this tree, so modifying the returned graph can 
	 * leave this tree in an illegal state if the resulting graph is not a 
	 * directed tree rooted in this tree's root node anymore.
	 *
	 * @return a directed graph representation of this tree.
	 */
	public DirectedGraph<V> asDirectedGraph() {
		return digraph;
	}

	/**
	 * Returns the path from a source to a destination vertex in this tree,
	 * if such a path exists. Otherwise, a path from the root to the destination
	 * vertex is returned.
	 *
	 * @param source the first vertex of the path
	 * @param destination the last vertex of the path
	 *
	 * @return a list of vertices, of which the first is the source and the
	 *         last is the destination vertex of the path. If no path
	 *         from the source to the destination vertex exists in this tree,
	 *         a path from the root to the destination vertex is returned.
	 *
	 * @throws NoSuchElementException if the destination vertex is not part of 
	 * this tree.
	 */
	public LinkedList<V> getPath(V source, V destination) 
	        throws NoSuchElementException {
		LinkedList<V> result = new LinkedList<V>();

		result.addLast(destination);
		V pivot = destination;

		while(!(pivot.equals(source) || pivot.equals(root))) {
			V parent = parent(pivot);
			result.addFirst(parent);
			pivot = parent;
		}

		return result;
	}

	/**
	 * Ensures that the specified path in reverse is part of this tree:
	 * for each edge (a, b) in the path the edge (parent(b), b) in this tree is 
	 * replaced by the edge (a, b). All related vertices in this tree of the 
	 * vertices in the path must also be part of the path, otherwise the state 
	 * of this tree may not be valid anymore. (A vertex a is related to a 
	 * vertex b if a is part of the subtree of b).
	 *
	 * @param path the path to generate in this tree
	 *
	 * @throws NoSuchElementException if a vertex in the path is not part of 
	 * this tree
	 */
	public void generatePath(List<V> path) {
		if (path.size() < 2) return;

		ListIterator<V> pathIt = path.listIterator();
		V a = pathIt.next();

		while(pathIt.hasNext()) {
			V b = pathIt.next();

			digraph.removeEdge(parent(b), b);
			digraph.addEdge(a, b);

			a = b;
		}
	}

	/**
	 * Returns a string representation of this tree.
	 *
	 * @return a string representation of this tree.
	 */
	public String toString() {
		String result = "";

		if (noVertices() == 1) {
			result = root.toString();
		} else {
			String treeConcat = "";

			LinkedList<V> todo = new LinkedList<V>();
			HashSet<V> done = new HashSet<V>();
			todo.addFirst(root);

			while(!todo.isEmpty()) {
				V vertex = todo.removeFirst();
				Set<V> children = children(vertex);
				if (!children.isEmpty()) {
					String childrenString = "";
					String childConcat = "";

					for (V child: children) {
					    if (!done.contains(child)) {
							childrenString += childConcat + child.toString();
							childConcat = ",";
							todo.addLast(child);
						}
					}

					result += treeConcat + vertex.toString() + "->{" + 
					        childrenString + "}";
					
					treeConcat = ",";
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
		
		DirectedRootedTree<?> rhs = (DirectedRootedTree<?>)other;
			
		return root.equals(rhs.root) && digraph.equals(rhs.digraph);
	}

}
