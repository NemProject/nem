package org.nem.peer.trust.score;

import org.nem.core.serialization.*;
import org.nem.peer.trust.PositiveLong;

/**
 * Represents experience one node has with another node.
 */
public class NodeExperience implements SerializableEntity {

	private PositiveLong successfulCalls = new PositiveLong(0);
	private PositiveLong failedCalls = new PositiveLong(0);

	/**
	 * Creates a new node experience.
	 */
	public NodeExperience() {
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
}
