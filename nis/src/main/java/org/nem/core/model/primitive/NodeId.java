package org.nem.core.model.primitive;

/**
 * Represents a node's id used in POI graph clustering.
 */
public class NodeId extends AbstractPrimitive<NodeId, Integer> {

	/**
	 * Creates a node id.
	 *
	 * @param nodeId The node id.
	 */
	public NodeId(final int nodeId) {
		super(nodeId, NodeId.class);

		if (this.getRaw() < 0) {
			throw new IllegalArgumentException("node id must be non-negative");
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
