package org.nem.core.time;

import org.nem.core.model.primitive.AbstractPrimitive;
import org.nem.core.serialization.*;

/**
 * Represents an time stamp in the NEM network in ms.
 */
public class NetworkTimeStamp extends AbstractPrimitive<NetworkTimeStamp, Long> {

	/**
	 * Creates a network time stamp.
	 *
	 * @param timeStamp The network time stamp.
	 */
	public NetworkTimeStamp(final long timeStamp) {
		super(timeStamp, NetworkTimeStamp.class);
	}

	/**
	 * Returns the underlying time stamp.
	 *
	 * @return The underlying time stamp.
	 */
	public Long getRaw() {
		return this.getValue();
	}

	/**
	 * Subtracts one network time stamp from another.
	 *
	 * @param other The other network time stamp.
	 * @return The difference in time stamps.
	 */
	public Long subtract(final NetworkTimeStamp other) {
		return this.getRaw() - other.getRaw();
	}

	//region inline serialization

	/**
	 * Writes a network time stamp object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param timeStamp The network time stamp.
	 */
	public static void writeTo(final Serializer serializer, final String label, final NetworkTimeStamp timeStamp) {
		serializer.writeLong(label, timeStamp.getRaw());
	}

	/**
	 * Reads a network time stamp object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static NetworkTimeStamp readFrom(final Deserializer deserializer, final String label) {
		return new NetworkTimeStamp(deserializer.readLong(label));
	}

	//endregion
}
