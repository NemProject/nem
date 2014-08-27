package org.nem.nis.time.synchronization;

import org.nem.core.crypto.KeyPair;
import org.nem.core.model.primitive.*;
import org.nem.core.node.*;

import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a time aware node in the network.
 */
public class TimeAwareNode {
	private static final Logger LOGGER = Logger.getLogger(TimeAwareNode.class.getName());

	public static final int NODE_TYPE_FRIENDLY = 1;
	public static final int NODE_TYPE_EVIL = 2;

	private final Node node;
	private final SynchronizationStrategy syncStrategy;
	private final long communicationDelay;
	private final double channelAsymmetry;
	private final long clockInaccuracy;
	private final int type;
	private long cumulativeInaccuracy;
	private NodeAge age;
	private long timeOffset = 0;
	private long updateCounter = 0;

	/**
	 * Creates a time aware node.
	 *
	 * @param id The id of the node.
	 * @param communicationDelay The delay to add to the communication send time stamp.
	 */
	public TimeAwareNode(
			final int id,
			final SynchronizationStrategy syncStrategy,
			final int initialTimeOffset,
			final long communicationDelay,
			final double channelAsymmetry,
			final long clockInaccuracy,
			final int type) {
		this.node = new Node(
				new NodeIdentity(new KeyPair(), String.format("node%d", id)),
				new NodeEndpoint("http", String.format("10.10.%d.%d", id / 256, id % 256), 12),
				null);
		this.syncStrategy = syncStrategy;
		this.timeOffset = initialTimeOffset;
		this.communicationDelay = communicationDelay;
		this.channelAsymmetry = channelAsymmetry;
		this.clockInaccuracy = clockInaccuracy;
		this.type = type;
		this.age = new NodeAge(0);
	}

	/**
	 * Gets the node's name.
	 *
	 * @return The node's name.
	 */
	public String getName() {
		return this.node.getIdentity().getName();
	}

	/**
	 * Gets the node's endpoint.
	 *
	 * @return The node's endpoint.
	 */
	public NodeEndpoint getEndpoint() {
		return this.node.getEndpoint();
	}

	/**
	 * Gets the node's network time.
	 *
	 * @return The network time.
	 */
	public NetworkTimeStamp getNetworkTime() {
		return new NetworkTimeStamp(System.currentTimeMillis() + this.timeOffset);
	}

	/**
	 * Updates the node's time offset.
	 *
	 * @param samples The list of synchronization samples.
	 */
	public void updateNetworkTime(final List<SynchronizationSample> samples) {
		try {
			final long diff = syncStrategy.calculateTimeOffset(samples, age);
			if (SynchronizationConstants.CLOCK_ADJUSTMENT_THRESHOLD < Math.abs(diff)) {
				this.timeOffset += diff;
			}
			this.age = this.age.increment();
		} catch (SynchronizationException e) {
			//LOGGER.info(e.toString());
			LOGGER.info(String.format("Resetting age of %s.", this.getName()));
			this.age = new NodeAge(0);
		}
	}

	/**
	 * Gets the node's age.
	 *
	 * @return The node age.
	 */
	public NodeAge getAge() {
		return this.age;
	}

	/**
	 * Gets the node's time offset value.
	 *
	 * @return The time offset.
	 */
	public long getTimeOffset() {
		return this.timeOffset;
	}

	/**
	 * Shifts the node's time offset by adding a value to it.
	 */
	public void shiftTimeOffset(final long offset) {
		this.timeOffset += offset;
	}

	/**
	 * Gets the node's communication delay value.
	 *
	 * @return The delay used for delaying the communication.
	 */
	public long getCommunicationDelay() {
		return this.communicationDelay;
	}

	/**
	 * Gets the inaccuracy of the node's clock.
	 *
	 * @return The inaccuracy.
	 */
	public long getClockInaccuary() {
		return this.clockInaccuracy;
	}

	public void applyClockInaccuracy() {
		this.timeOffset += this.clockInaccuracy;
		this.cumulativeInaccuracy += this.clockInaccuracy;
	}

	/**
	 * Returns a value indicating whether or not the node is evil.
	 *
	 * @return true if node is evil, false otherwise.
	 */
	public boolean isEvil() {
		return this.type == NODE_TYPE_EVIL;
	}

	/**
	 * Decrements and returns the update counter.
	 *
	 * @param decrement The value to subtract from the update counter.
	 *
	 * @return The decremented update counter.
	 */
	public long decrementUpdateCounter(final long decrement) {
		this.updateCounter -= decrement;

		return this.updateCounter;
	}

	public void setUpdateCounter(final long initialValue) {
		this.updateCounter = initialValue;
	}

	/**
	 * Creates a new communication time stamps object based on current time offset and delay.
	 *
	 * @return The communication time stamps.
	 */
	public CommunicationTimeStamps createCommunicationTimeStamps(final int roundTripTime) {
		if (isEvil()) {
			return new CommunicationTimeStamps(
					new NetworkTimeStamp(System.currentTimeMillis() + 30000),
					new NetworkTimeStamp(System.currentTimeMillis() + 30000));
		}

		return new CommunicationTimeStamps(
				new NetworkTimeStamp(System.currentTimeMillis() + this.timeOffset + (long)(roundTripTime * this.channelAsymmetry) + this.communicationDelay),
				new NetworkTimeStamp(System.currentTimeMillis() + this.timeOffset + (long)(roundTripTime * this.channelAsymmetry)));
	}

	/**
	 * Adjusts the clock by subtracting the cumulative inaccuracy that the clock experienced so far.
	 */
	public void adjustClock() {
		this.timeOffset -= this.cumulativeInaccuracy;
		this.cumulativeInaccuracy = 0;
	}
}
