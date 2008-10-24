package mcast.ht.graph;

import java.io.Serializable;

/**
 * This class implements an immutable, directed edge in an directed graph.
 * An edge consists of a source and destination vertex and is annotated with
 * some data. The data can be <code>null</code>.
 */
public class Edge<V> implements Serializable {

	public V source, destination;
	public Object data;

	/**
	 * Creates a new edge from the source to the destination node.
	 *
	 * @param source the source node of the edge
	 * @param destination the destination node of the edge
	 */
	Edge(V source, V destination) {
		this(source, destination, null);
	}

	/**
	 * Creates a new edge from the source to the destination node,
	 * annotated with some data.
	 *
	 * @param source the source node of the edge
	 * @param destination the destination node of the edge
	 * @param data the data the edge is annotated with (can be 
	 * <code>null</code>)
	 */
	Edge(V source, V destination, Object data) {
		this.source = source;
		this.destination = destination;
		this.data = data;
	}

	/**
	 * Creates a shallow copy of the specified edge.
	 *
	 * @param original the original edge
	 */
	Edge(Edge<V> original) {
		this.source = original.source;
		this.destination = original.destination;
		this.data = original.data;
	}

	/**
	 * Compares this edges with the specified object for equality. This edge
	 * is equal to the specified object if that object is also an
	 * <code>Edge</code>, and the source and destination vertices
	 * of both edges are also equal to each other.
	 *
	 * @return <code>true</code> if this edge is equal to the specified object,
	 *         <code>false</code> if not.
	 */
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (other == null) {
			return false;
		} else if (this.getClass() != other.getClass()) {
			return false;
		}
		
		Edge<?> rhs = (Edge<?>)other;
			
		return ((source == null && rhs.source == null) || 
		        source.equals(rhs.source)) && 
		            ((destination == null && rhs.destination == null) || 
		             destination.equals(rhs.destination));
	}

	/**
	 * Returns a hash code value for this edge. This value only depends on
	 * the hash code values of the source and destination vertices.
	 *
	 * @return a hash code value for this edge
	 */
	public int hashCode() {
		int result = 0;
		if (source != null) result += source.hashCode();
		if (destination != null) result += destination.hashCode();
		return result;
	}

	/**
	 * Returns a string representation of this edge
	 *
	 * @return a string representation of this edge
	 */
	public String toString() {
		return "(" + source + "->" + destination + ": " + data + ")";
	}

	/**
	 * Returns a shallow copy of this edge.
	 *
	 * @return a shallow copy of this edge.
	 */
	public Object clone() {
		return new Edge<V>(this);
	}

}
