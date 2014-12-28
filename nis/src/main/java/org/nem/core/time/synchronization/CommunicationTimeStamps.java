package org.nem.core.time.synchronization;

import org.nem.core.serialization.*;
import org.nem.core.time.NetworkTimeStamp;

/**
 * Represents the network time stamps for sending and receiving a time synchronization request/response.
 */
public class CommunicationTimeStamps implements SerializableEntity {
	private final NetworkTimeStamp sendTimeStamp;
	private final NetworkTimeStamp receiveTimeStamp;

	/**
	 * Creates a new communication time stamps object.
	 *
	 * @param sendTimeStamp The time when the request/response was sent.
	 * @param receiveTimeStamp The time when the request/response was received.
	 */
	public CommunicationTimeStamps(final NetworkTimeStamp sendTimeStamp, final NetworkTimeStamp receiveTimeStamp) {
		this.sendTimeStamp = sendTimeStamp;
		this.receiveTimeStamp = receiveTimeStamp;
	}

	/**
	 * Deserializes a communication time stamps object.
	 *
	 * @param deserializer The deserializer.
	 */
	public CommunicationTimeStamps(final Deserializer deserializer) {
		this.sendTimeStamp = NetworkTimeStamp.readFrom(deserializer, "sendTimeStamp");
		this.receiveTimeStamp = NetworkTimeStamp.readFrom(deserializer, "receiveTimeStamp");
	}

	/**
	 * Gets the network time stamp when the request/response was sent.
	 *
	 * @return The send time stamp.
	 */
	public NetworkTimeStamp getSendTimeStamp() {
		return this.sendTimeStamp;
	}

	/**
	 * Gets the network time stamp when the request/response was received.
	 *
	 * @return The receive time stamp.
	 */
	public NetworkTimeStamp getReceiveTimeStamp() {
		return this.receiveTimeStamp;
	}

	@Override
	public void serialize(final Serializer serializer) {
		NetworkTimeStamp.writeTo(serializer, "sendTimeStamp", this.sendTimeStamp);
		NetworkTimeStamp.writeTo(serializer, "receiveTimeStamp", this.receiveTimeStamp);
	}

	@Override
	public int hashCode() {
		return this.sendTimeStamp.getRaw().hashCode() ^ this.receiveTimeStamp.getRaw().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof CommunicationTimeStamps)) {
			return false;
		}

		final CommunicationTimeStamps rhs = (CommunicationTimeStamps)obj;
		return this.sendTimeStamp.equals(rhs.sendTimeStamp)
				&& this.receiveTimeStamp.equals(rhs.receiveTimeStamp);
	}
}
