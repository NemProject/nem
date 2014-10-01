package org.nem.nis.poi.graph;

import org.nem.core.model.primitive.NodeId;

/**
 * A <code>Community</code> is centered around a pivot. <i>Epsilon</i> neighbors are
 * connected to each other with a structural similarity greater than a given
 * <code>epsilon</code>.
 */
public class Community {
	private final NodeId pivotId;
	private final NodeNeighbors similarNeighbors;
	private final NodeNeighbors dissimilarNeighbors;

	/**
	 * Creates a new community.
	 *
	 * @param pivotId The pivot node id.
	 * @param similarNeighbors The similar node neighbors.
	 * @param dissimilarNeighbors The dissimilar node neighbors.
	 */
	public Community(
			final NodeId pivotId,
			final NodeNeighbors similarNeighbors,
			final NodeNeighbors dissimilarNeighbors) {
		// TODO 20140930 J-M should this constructor ensure that pivotId is contained within similar Neighbors?
		if (null == similarNeighbors || null == dissimilarNeighbors) {
			throw new IllegalArgumentException("neighbors cannot be null");
		}

		this.pivotId = pivotId;
		this.similarNeighbors = similarNeighbors;
		this.dissimilarNeighbors = dissimilarNeighbors;
	}

	/**
	 * Creates a new isolated community.
	 *
	 * @param pivotId The pivot node id.
	 */
	public Community(final NodeId pivotId) {
		this(pivotId, new NodeNeighbors(pivotId), new NodeNeighbors());
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
		return this.similarNeighbors.size() >= GraphConstants.MU;
	}

	/**
	 * Gets the total number of nodes in this community.
	 *
	 * @return Total number of nodes in this cluster.
	 */
	public int size() {
		return this.similarNeighbors.size() + this.dissimilarNeighbors.size();
	}

	//region hashCode / equals

	@Override
	public int hashCode() {
		return this.pivotId.getRaw();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Community)) {
			return false;
		}

		final Community rhs = (Community)obj;
		return this.pivotId.equals(rhs.pivotId) &&
				this.similarNeighbors.equals(rhs.similarNeighbors) &&
				this.dissimilarNeighbors.equals(rhs.dissimilarNeighbors);
	}

	// endregion

	@Override
	public String toString() {
		return String.format("Pivot Id: %d; Similar Neighbor Ids: %s; Dissimilar Neighbor Ids: %s",
				this.pivotId.getRaw(),
				this.similarNeighbors.toString(),
				this.dissimilarNeighbors.toString());
	}
}
