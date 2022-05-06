package org.nem.core.model.primitive;

/**
 * Represents the age of a node with respect to time synchronization.
 */
public class NodeAge extends AbstractPrimitive<NodeAge, Long> {

	/**
	 * Creates a node age.
	 *
	 * @param age The node's age.
	 */
	public NodeAge(final long age) {
		super(age, NodeAge.class);

		if (this.getRaw() < 0) {
			throw new IllegalArgumentException("node age cannot be negative");
		}
	}

	/**
	 * Returns the underlying age.
	 *
	 * @return The underlying age.
	 */
	public long getRaw() {
		return this.getValue();
	}

	/**
	 * Increments the node's age.
	 *
	 * @return The incremented node age.
	 */
	public NodeAge increment() {
		return new NodeAge(this.getRaw() + 1);
	}
}
