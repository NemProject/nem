package org.nem.nis.time.synchronization;

import org.nem.core.node.NodeEndpoint;

/**
 * Represents a sample in the time synchronization process.
 */
public class SynchronizationSample implements Comparable<SynchronizationSample> {

	private final NodeEndpoint endpoint;
	private final CommunicationTimeStamps localTimeStamps;
	private final CommunicationTimeStamps remoteTimeStamps;

	/**
	 * Creates a SynchronizationSample.
	 *
	 * @param endpoint The endpoint of the communication partner.
	 * @param localTimeStamps The local time stamps for the request/response.
	 * @param remoteTimeStamps The remote time stamps for the request/response.
	 */
	public SynchronizationSample(
			final NodeEndpoint endpoint,
			final CommunicationTimeStamps localTimeStamps,
			final CommunicationTimeStamps remoteTimeStamps) {
		this.endpoint = endpoint;
		this.localTimeStamps = localTimeStamps;
		this.remoteTimeStamps = remoteTimeStamps;
	}

	/**
	 * Gets the endpoint of the communication partner.
	 *
	 * @return The endpoint.
	 */
	NodeEndpoint getEndpoint() {
		return this.endpoint;
	}

	/**
	 * Gets the local time stamps for the request/response.
	 *
	 * @return The local time stamps.
	 */
	CommunicationTimeStamps getLocalTimeStamps() {
		return this.localTimeStamps;
	}

	/**
	 * Gets the remote time stamps for the request/response.
	 *
	 * @return The remote time stamps.
	 */
	CommunicationTimeStamps getRemoteTimeStamps() {
		return this.remoteTimeStamps;
	}

	/**
	 * Gets the offset that the local node's network time has to the remote node's network time.
	 *
	 * S=Send, R=Receive
	 *
	 * remote node   ----------R------S------->
	 *                        o        \
	 *                      /           \    time
	 *                    /              o
	 * local node    ---S-----------------R--->
	 *
	 * @return The offset in ms.
	 */
	public Long getTimeOffsetToRemote() {
		final long roundTripTime =
				localTimeStamps.getReceiveTimeStamp().subtract(localTimeStamps.getSendTimeStamp()) -
				remoteTimeStamps.getSendTimeStamp().subtract(remoteTimeStamps.getReceiveTimeStamp());

		//TODO-CR J-B consider storing remoteTimeStamps.getReceiveTimeStamp().subtract(localTimeStamps.getSendTimeStamp()) in a variable since you're calculating it twice
		return remoteTimeStamps.getReceiveTimeStamp().subtract(localTimeStamps.getSendTimeStamp()) - roundTripTime/2;
	}

	@Override
	public int compareTo(final SynchronizationSample other) {
		return this.getTimeOffsetToRemote().compareTo(other.getTimeOffsetToRemote());
	}

	@Override
	public int hashCode() {
		return this.localTimeStamps.hashCode() ^ this.remoteTimeStamps.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SynchronizationSample)) {
			return false;
		}

		final SynchronizationSample rhs = (SynchronizationSample)obj;
		return this.localTimeStamps.equals(rhs.localTimeStamps)
				&& this.remoteTimeStamps.equals(rhs.remoteTimeStamps);
	}
}
