package org.nem.nis.time.synchronization;

import org.nem.core.crypto.KeyPair;
import org.nem.core.model.primitive.*;
import org.nem.core.node.*;
import org.nem.core.time.NetworkTimeStamp;
import org.nem.core.time.synchronization.*;

import java.util.List;

/**
 * Represents a time aware node in the network.
 */
public class TimeAwareNode {
	private static final int CLOCK_ADJUSTMENT_THRESHOLD = 75;
	public static final int NODE_TYPE_FRIENDLY = 1;
	public static final int NODE_TYPE_EVIL = 2;

	private final Node node;
	private NodeAge age;
	private final TimeSynchronizationStrategy syncStrategy;
	private final TimeOffset communicationDelay;
	private final TimeOffset clockInaccuracy;
	private TimeOffset cumulativeInaccuracy = new TimeOffset(0);
	private TimeOffset timeOffset = new TimeOffset(0);
	private final double channelAsymmetry;
	private final int type;
	private long updateCounter = 0;

	/**
	 * Creates a time aware node.
	 *
	 * @param id The id of the node.
	 * @param nodeAge The age of the node.
	 * @param syncStrategy The synchronization strategy to use.
	 * @param initialTimeOffset The initial time offset.
	 * @param communicationDelay The delay to add to the communication send time stamp.
	 * @param channelAsymmetry The channel asymmetry (must be between 0.0 and 1.0).
	 * @param clockInaccuracy The value to add to the time offset every hour.
	 * @param type The node's type (friendly or evil).
	 */
	public TimeAwareNode(
			final int id,
			final NodeAge nodeAge,
			final TimeSynchronizationStrategy syncStrategy,
			final TimeOffset initialTimeOffset,
			final TimeOffset communicationDelay,
			final double channelAsymmetry,
			final TimeOffset clockInaccuracy,
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
		this.age = nodeAge;
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
	 * Gets the nem node.
	 *
	 * @return The node.
	 */
	public Node getNode() {
		return this.node;
	}

	/**
	 * Gets the node's network time.
	 *
	 * @return The network time.
	 */
	public NetworkTimeStamp getNetworkTime() {
		return new NetworkTimeStamp(System.currentTimeMillis() + this.timeOffset.getRaw());
	}

	/**
	 * Updates the node's time offset.
	 *
	 * @param samples The list of synchronization samples.
	 */
	public void updateNetworkTime(final List<TimeSynchronizationSample> samples) {
		try {
			final TimeOffset diff = this.syncStrategy.calculateTimeOffset(samples, this.age);
			if (CLOCK_ADJUSTMENT_THRESHOLD < Math.abs(diff.getRaw())) {
				this.timeOffset = this.timeOffset.add(diff);
			}
			this.age = this.age.increment();
		} catch (final TimeSynchronizationException e) {
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
	public TimeOffset getTimeOffset() {
		return this.timeOffset;
	}

	/**
	 * Shifts the node's time offset by adding a value to it.
	 */
	public void shiftTimeOffset(final TimeOffset offset) {
		this.timeOffset = this.timeOffset.add(offset);
	}

	/**
	 * Gets the node's communication delay value.
	 *
	 * @return The delay used for delaying the communication.
	 */
	public TimeOffset getCommunicationDelay() {
		return this.communicationDelay;
	}

	/**
	 * Gets the node's channel asymmetry value.
	 *
	 * @return The channel asymmetry.
	 */
	public double getChannelAsymmetry() {
		return this.channelAsymmetry;
	}

	/**
	 * Gets the inaccuracy of the node's clock.
	 *
	 * @return The inaccuracy.
	 */
	public TimeOffset getClockInaccuary() {
		return this.clockInaccuracy;
	}

	public void applyClockInaccuracy() {
		this.timeOffset = this.timeOffset.add(this.clockInaccuracy);
		this.cumulativeInaccuracy = this.cumulativeInaccuracy.add(this.clockInaccuracy);
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
		if (this.isEvil()) {
			return new CommunicationTimeStamps(
					new NetworkTimeStamp(System.currentTimeMillis() + this.timeOffset.getRaw() + 30000),
					new NetworkTimeStamp(System.currentTimeMillis() + this.timeOffset.getRaw() + 30000));
		}

		return new CommunicationTimeStamps(
				new NetworkTimeStamp(System.currentTimeMillis() + this.timeOffset.getRaw() + (long)(roundTripTime * this.channelAsymmetry) +
						this.communicationDelay.getRaw()),
				new NetworkTimeStamp(System.currentTimeMillis() + this.timeOffset.getRaw() + (long)(roundTripTime * this.channelAsymmetry)));
	}

	/**
	 * Adjusts the clock by subtracting the cumulative inaccuracy that the clock experienced so far.
	 */
	public void adjustClock() {
		this.timeOffset = this.timeOffset.subtract(this.cumulativeInaccuracy);
		this.cumulativeInaccuracy = new TimeOffset(0);
	}
}
