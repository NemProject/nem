package org.nem.nis.pox.poi.graph;

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
	 * Creates a new NodeNeighbors object. <em>The neighbor ids MUST be sorted in ascending order.</em>
	 *
	 * @param neighborIds The (sorted) neighbor ids.
	 */
	public NodeNeighbors(final NodeId... neighborIds) {
		this.neighborIds = SparseBitmap.createEmpty();
		if (neighborIds.length > 0) {
			this.maxId = neighborIds[0].getRaw();
			for (final NodeId nodeId : neighborIds) {
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
	 *
	 * @return The number of neighbors.
	 */
	public int size() {
		return this.neighborIds.cardinality();
	}

	/**
	 * Adds a new neighbor. <em>Adding new neighbors MUST be in ascending order of ids.</em>
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
	 * Gets the number neighbors common to this node's neighbors and another node's neighbors.
	 *
	 * @param otherNodeNeighbors the other node's neighbors.
	 * @return The number of common neighbors.
	 */
	public int commonNeighborsSize(final NodeNeighbors otherNodeNeighbors) {
		return this.neighborIds.andCardinality(otherNodeNeighbors.neighborIds);
	}

	/**
	 * Unions all the node neighbors together.
	 *
	 * @param nodeNeighborsArray The array of node neighbors.
	 * @return The union of all node neighbors.
	 */
	public static NodeNeighbors union(final NodeNeighbors... nodeNeighborsArray) {
		SparseBitmap bitmap = SparseBitmap.createEmpty();
		for (final NodeNeighbors neighbors : nodeNeighborsArray) {
			bitmap = bitmap.or(neighbors.neighborIds);
		}

		return new NodeNeighbors(bitmap);
	}

	/**
	 * Returns the set difference of this node neighbors and other node neighbors.
	 *
	 * @param other The other node neighbors.
	 * @return The set difference.
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
				if (!this.hasNext()) {
					throw new IndexOutOfBoundsException("index out of range");
				}

				return new NodeId(this.neighborIdsIterator.next());
			}

			private final Iterator<Integer> neighborIdsIterator = NodeNeighbors.this.neighborIds.iterator();
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

		final NodeNeighbors rhs = (NodeNeighbors) obj;
		return this.neighborIds.equals(rhs.neighborIds);
	}
}
