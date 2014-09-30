package org.nem.nis.poi.graph;

import org.nem.core.math.SparseBitmap;
import org.nem.core.model.primitive.NodeId;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a set of neighbors for a node.
 */
public class NodeNeighbors implements Iterable<NodeId> {
	private final SparseBitmap neighborIds;
	private int maxId;

	/**
	 * Creates a new NodeNeighbors object.
	 * The neighbor ids MUST be sorted in ascending order.
	 *
	 * @param neighborIds The (sorted) neighbor ids.
	 */
	public NodeNeighbors(final NodeId... neighborIds) {
		this.neighborIds = SparseBitmap.createFromSortedData();
		if (neighborIds.length > 0) {
			this.maxId = neighborIds[0].getRaw();
			for (NodeId nodeId : neighborIds) {
				if (this.maxId > nodeId.getRaw()) {
					throw new IllegalArgumentException("ids must be set in strictly ascending order");
				}
				this.maxId = nodeId.getRaw();
				this.addNeighbor(nodeId);
			}
		}
	}

	private NodeNeighbors(final SparseBitmap bitmap) {
		this.neighborIds = bitmap;
		this.maxId = this.neighborIds.getHighestBit();
	}

	/**
	 * Gets the number of neighbors.
	 */
	public int size() {
		return this.neighborIds.cardinality();
	}

	/**
	 * Adds a new neighbor.
	 * Adding new neighbors MUST be in ascending order of ids.
	 *
	 * @param nodeId The node id.
	 */
	public void addNeighbor(final NodeId nodeId) {
		final int id = nodeId.getRaw();
		if (this.maxId > id) {
			throw new IllegalArgumentException("ids must be set in strictly ascending order");
		}
		this.maxId = id;
		this.neighborIds.setWithoutAscendingCheck(id);
	}

	/**
	 * Removes all neighbors.
	 */
	public void removeAll() {
		this.neighborIds.clear();
		this.maxId = 0;
	}

	/**
	 * Gets a value indicating if the given node id is contained in the neighbor ids.
	 *
	 * @param id The node id.
	 * @return true if the neighbor ids contain the given node id, false otherwise.
	 */
	public boolean contains(final NodeId id) {
		return this.neighborIds.get(id.getRaw());
	}

	/**
	 * Gets the number of common neighbors of this node's neighbors and another node's neighbors.
	 *
	 * @param otherNodeNeighbors the other node's neighbors.
	 * @return The number of common neighbors.
	 */
	public int commonNeighborsSize(final NodeNeighbors otherNodeNeighbors) {
		return this.neighborIds.andCardinality(otherNodeNeighbors.neighborIds);
	}

	/**
	 * Creates the union of this node neighbors with the other node neighbors.
	 *
	 * @param other The node neighbors.
	 * @return The union with the other node neighbors.
	 */
	public NodeNeighbors union(final NodeNeighbors other) {
		return union(new NodeNeighbors[] { other });
	}

	/**
	 * Creates the union of this node neighbors with an array of other node neighbors.
	 *
	 * @param nodeNeighborsArray The array of node neighbors.
	 * @return The union of all node neighbors.
	 */
	public NodeNeighbors union(final NodeNeighbors[] nodeNeighborsArray) {
		SparseBitmap bitmap = this.neighborIds;
		for (NodeNeighbors neighbors : nodeNeighborsArray) {
			bitmap = bitmap.or(neighbors.neighborIds);
		}

		return new NodeNeighbors(bitmap);
	}

	/**
	 * Returns the set theoretic difference of this node neighbors and other node neighbors.
	 *
	 * @param other The other node neighbors.
	 * @return The set theoretic difference.
	 */
	public NodeNeighbors difference(final NodeNeighbors other) {
		return new NodeNeighbors(this.neighborIds.andNot(other.neighborIds));
	}

	/**
	 * Gets the list of neighbor ids.
	 *
	 * @return The list of neighbor ids.
	 */
	public List<NodeId> toList() {
		return this.neighborIds.toList().stream().map(NodeId::new).collect(Collectors.toList());
	}

	/**
	 * Iterates over the neighbor ids.
	 *
	 * @return The iterator.
	 */
	@Override
	public Iterator<NodeId> iterator() {
		return new Iterator<NodeId>() {
			@Override
			public boolean hasNext() {
				return this.neighborIdsIterator.hasNext();
			}

			@Override
			public NodeId next() {
				return new NodeId(this.neighborIdsIterator.next());
			}

			private final Iterator<Integer> neighborIdsIterator = neighborIds.iterator();
		};
	}

	@Override
	public String toString() {
		return this.neighborIds.toString();
	}

	@Override
	public int hashCode() {
		return this.neighborIds.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NodeNeighbors)) {
			return false;
		}

		final NodeNeighbors rhs = (NodeNeighbors)obj;
		return this.neighborIds.equals(rhs.neighborIds);
	}
}
