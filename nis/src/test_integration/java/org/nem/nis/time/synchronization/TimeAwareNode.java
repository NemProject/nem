package org.nem.nis.time.synchronization;

import org.nem.core.model.primitive.*;
import org.nem.core.node.NodeEndpoint;

import java.util.List;

/**
 * Represents a time aware node in the network..
 */
public class TimeAwareNode {

	private final String name;
	private final NodeEndpoint endpoint;
	private final SynchronizationStrategy syncStrategy;
	private NodeAge age;
	private long timeOffset = 0;
	private long communicationDelay = 0;

	/**
	 * Creates a time aware node.
	 *
	 * @param id The id of the node.
	 * @param communicationDelay The delay to add to the communication send time stamp.
	 */
	public TimeAwareNode(
			final int id,
			final SynchronizationStrategy syncStrategy,
			final long communicationDelay) {
		this.name = String.format("node%d", id);
		this.endpoint = new NodeEndpoint("http", String.format("10.10.%d.%d", id / 256, id % 256), 12);
		this.syncStrategy = syncStrategy;
		this.communicationDelay = communicationDelay;
		this.age = new NodeAge(0);
	}

	/**
	 * Gets the node's name.
	 *
	 * @return The node's name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the node's endpoint.
	 *
	 * @return The node's endpoint.
	 */
	public NodeEndpoint getEndpoint() {
		return this.endpoint;
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
	public void updateNetworkTime(List<SynchronizationSample> samples) {
		this.timeOffset = syncStrategy.calculateTimeOffset(samples, age);
		this.age = this.age.increment();
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
	 * Creates a new communication time stamps object based on current time offset and delay.
	 *
	 * @return The communication time stamps.
	 */
	public CommunicationTimeStamps createCommunicationTimeStamps() {
		return new CommunicationTimeStamps(
				new NetworkTimeStamp(System.currentTimeMillis() + this.timeOffset + this.communicationDelay),
				this.getNetworkTime());
	}
}
