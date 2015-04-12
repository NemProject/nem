package org.nem.peer.trust.score;

import org.nem.core.serialization.*;

/**
 * Represents experience one node has with another node.
 */
public class NodeExperience implements SerializableEntity {
	private final PositiveLong successfulCalls = new PositiveLong(0);
	private final PositiveLong failedCalls = new PositiveLong(0);

	/**
	 * Creates a new node experience with default values.
	 */
	public NodeExperience() {
	}

	/**
	 * Creates a new node experience with initial values.
	 *
	 * @param successfulCalls The number of successful calls.
	 * @param failedCalls The number of failed calls.
	 */
	public NodeExperience(final long successfulCalls, final long failedCalls) {
		this.successfulCalls().set(successfulCalls);
		this.failedCalls().set(failedCalls);
	}

	/**
	 * Deserializes a new node experience.
	 *
	 * @param deserializer The deserializer
	 */
	public NodeExperience(final Deserializer deserializer) {
		this.successfulCalls().set(deserializer.readLong("s"));
		this.failedCalls().set(deserializer.readLong("f"));
	}

	/**
	 * Gets the number of successful calls.
	 *
	 * @return The number of successful calls.
	 */
	public PositiveLong successfulCalls() {
		return this.successfulCalls;
	}

	/**
	 * Gets the number of failed calls.
	 *
	 * @return The number of failed calls.
	 */
	public PositiveLong failedCalls() {
		return this.failedCalls;
	}

	/**
	 * Gets the total number of calls.
	 *
	 * @return The total number of calls.
	 */
	public long totalCalls() {
		return this.successfulCalls.get() + this.failedCalls().get();
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeLong("s", this.successfulCalls.get());
		serializer.writeLong("f", this.failedCalls().get());
	}

	@Override
	public int hashCode() {
		return Long.valueOf(this.successfulCalls().get() ^ this.failedCalls().get()).intValue();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NodeExperience)) {
			return false;
		}

		final NodeExperience rhs = (NodeExperience)obj;
		return this.successfulCalls.get() == rhs.successfulCalls().get() &&
				this.failedCalls().get() == rhs.failedCalls().get();
	}

	@Override
	public String toString() {
		return String.format(
				"success: %d, failure: %d",
				this.successfulCalls().get(),
				this.failedCalls().get());
	}
}
