package org.nem.core.model.primitive;

/**
 * Represents a cluster's id used in POI graph clustering.
 */
public class ClusterId extends AbstractPrimitive<ClusterId, Integer> {

	/**
	 * Creates a cluster id.
	 *
	 * @param clusterId The cluster id.
	 */
	public ClusterId(final int clusterId) {
		super(clusterId, ClusterId.class);

		if (this.getRaw() < 0) {
			throw new IllegalArgumentException("cluster id must be non-negative");
		}
	}

	/**
	 * Creates a cluster id from a node id.
	 *
	 * @param nodeId The node id.
	 */
	public ClusterId(final NodeId nodeId) {
		super(nodeId.getRaw(), ClusterId.class);

		if (this.getRaw() < 0) {
			throw new IllegalArgumentException("cluster id must be non-negative");
		}
	}

	/**
	 * Returns the underlying id.
	 *
	 * @return The underlying id.
	 */
	public int getRaw() {
		return this.getValue();
	}
}
