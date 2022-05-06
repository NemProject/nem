package org.nem.nis.pox.poi.graph;

import org.nem.core.model.primitive.NodeId;

/**
 * A <code>Community</code> is centered around a pivot. <i>Epsilon</i> neighbors are connected to each other with a structural similarity
 * greater than a given <code>epsilon</code>.
 */
public class Community {
	private final NodeId pivotId;
	private final NodeNeighbors similarNeighbors;
	private final NodeNeighbors dissimilarNeighbors;
	private final int mu;

	/**
	 * Creates a new community.
	 *
	 * @param pivotId The pivot node id.
	 * @param similarNeighbors The similar node neighbors.
	 * @param dissimilarNeighbors The dissimilar node neighbors.
	 * @param mu The minimum number of neighbors with high structural similarity that a core community must have.
	 */
	public Community(final NodeId pivotId, final NodeNeighbors similarNeighbors, final NodeNeighbors dissimilarNeighbors, final int mu) {
		if (null == similarNeighbors || null == dissimilarNeighbors) {
			throw new IllegalArgumentException("neighbors cannot be null");
		}

		if (!similarNeighbors.contains(pivotId)) {
			throw new IllegalArgumentException("similar neighbors must contain pivot");
		}

		this.pivotId = pivotId;
		this.similarNeighbors = similarNeighbors;
		this.dissimilarNeighbors = dissimilarNeighbors;
		this.mu = mu;
	}

	/**
	 * Creates a new isolated community.
	 *
	 * @param pivotId The pivot node id.
	 * @param mu The minimum number of neighbors with high structural similarity that a core community must have.
	 */
	public Community(final NodeId pivotId, final int mu) {
		this(pivotId, new NodeNeighbors(pivotId), new NodeNeighbors(), mu);
	}

	/**
	 * Gets the node id of the pivot (the center node).
	 *
	 * @return The pivot node id.
	 */
	public NodeId getPivotId() {
		return this.pivotId;
	}

	/**
	 * Gets the ids of all similar neighbors (nodes connected to the pivot that are more similar than a predefined value).
	 *
	 * @return The similar neighbors.
	 */
	public NodeNeighbors getSimilarNeighbors() {
		return this.similarNeighbors;
	}

	/**
	 * Gets the ids of all dissimilar neighbors (nodes connected to the pivot that are less similar than a predefined value).
	 *
	 * @return The dissimilar neighbors.
	 */
	public NodeNeighbors getDissimilarNeighbors() {
		return this.dissimilarNeighbors;
	}

	/**
	 * Gets a value indicating whether or not the community is isolated.
	 *
	 * @return true if this community is isolated.
	 */
	public boolean isIsolated() {
		return this.similarNeighbors.size() == 1 && this.dissimilarNeighbors.size() == 0;
	}

	/**
	 * Gets a value indicating whether or not the community is core.
	 *
	 * @return true if this community is core.
	 */
	public boolean isCore() {
		return this.similarNeighbors.size() >= this.mu;
	}

	/**
	 * Gets the total number of nodes in this community.
	 *
	 * @return Total number of nodes in this cluster.
	 */
	public int size() {
		return this.similarNeighbors.size() + this.dissimilarNeighbors.size();
	}

	// region hashCode / equals

	@Override
	public int hashCode() {
		return this.similarNeighbors.size();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Community)) {
			return false;
		}

		// since the pivot is guaranteed to be one of the similar neighbors, and there's nothing special about
		// the chosen "pivot", don't check the pivot for equality
		final Community rhs = (Community) obj;
		return this.similarNeighbors.equals(rhs.similarNeighbors) && this.dissimilarNeighbors.equals(rhs.dissimilarNeighbors);
	}

	// endregion

	@Override
	public String toString() {
		return String.format("Pivot Id: %d; Similar Neighbor Ids: %s; Dissimilar Neighbor Ids: %s", this.pivotId.getRaw(),
				this.similarNeighbors.toString(), this.dissimilarNeighbors.toString());
	}
}
