package org.nem.core.time.synchronization;

import org.nem.core.node.Node;

/**
 * Represents a sample in the time synchronization process.
 */
public class TimeSynchronizationSample implements Comparable<TimeSynchronizationSample> {

	private final Node node;
	private final CommunicationTimeStamps localTimeStamps;
	private final CommunicationTimeStamps remoteTimeStamps;

	/**
	 * Creates a TimeSynchronizationSample.
	 *
	 * @param node The communication partner.
	 * @param localTimeStamps The local time stamps for the request/response.
	 * @param remoteTimeStamps The remote time stamps for the request/response.
	 */
	public TimeSynchronizationSample(
			final Node node,
			final CommunicationTimeStamps localTimeStamps,
			final CommunicationTimeStamps remoteTimeStamps) {
		this.node = node;
		this.localTimeStamps = localTimeStamps;
		this.remoteTimeStamps = remoteTimeStamps;
	}

	/**
	 * Gets the communication partner.
	 *
	 * @return The node.
	 */
	public Node getNode() {
		return this.node;
	}

	/**
	 * Gets the local time stamps for the request/response.
	 *
	 * @return The local time stamps.
	 */
	public CommunicationTimeStamps getLocalTimeStamps() {
		return this.localTimeStamps;
	}

	/**
	 * Gets the remote time stamps for the request/response.
	 *
	 * @return The remote time stamps.
	 */
	public CommunicationTimeStamps getRemoteTimeStamps() {
		return this.remoteTimeStamps;
	}

	/**
	 * Gets the duration of the complete cycle.
	 *
	 * @return The duration.
	 */
	public Long getDuration() {
		return this.getLocalTimeStamps().getReceiveTimeStamp().subtract(this.getLocalTimeStamps().getSendTimeStamp());
	}

	/**
	 * Gets the offset that the local node's network time has to the remote node's network time.
	 * <pre>
	 * {@code
	 * S=Send, R=Receive
	 * remote node   ----------R------S------->
	 *                        o        \
	 *                      /           \    time
	 *                    /              o
	 * local node    ---S-----------------R--->
	 * }
	 * </pre>
	 *
	 * @return The offset in ms.
	 */
	public Long getTimeOffsetToRemote() {
		final long roundTripTime =
				this.localTimeStamps.getReceiveTimeStamp().subtract(this.localTimeStamps.getSendTimeStamp()) -
						this.remoteTimeStamps.getSendTimeStamp().subtract(this.remoteTimeStamps.getReceiveTimeStamp());

		return this.remoteTimeStamps.getReceiveTimeStamp().subtract(this.localTimeStamps.getSendTimeStamp()) - roundTripTime / 2;
	}

	@Override
	public int compareTo(final TimeSynchronizationSample other) {
		return this.getTimeOffsetToRemote().compareTo(other.getTimeOffsetToRemote());
	}

	@Override
	public int hashCode() {
		return this.node.hashCode() ^
				this.localTimeStamps.hashCode() ^
				this.remoteTimeStamps.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof TimeSynchronizationSample)) {
			return false;
		}

		final TimeSynchronizationSample rhs = (TimeSynchronizationSample)obj;
		return this.node.equals(rhs.node) &&
				this.localTimeStamps.equals(rhs.localTimeStamps) &&
				this.remoteTimeStamps.equals(rhs.remoteTimeStamps);
	}
}
