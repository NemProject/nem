package org.nem.core.time;

import org.nem.core.model.primitive.TimeOffset;
import org.nem.core.serialization.*;

/**
 * Information about the result of a time synchronization with other nodes.
 */
public class TimeSynchronizationResult implements SerializableEntity {

	private final TimeInstant timeStamp;
	private final TimeOffset currentTimeOffset;
	private final TimeOffset change;

	/**
	 * Creates a time synchronization result object.
	 *
	 * @param timeStamp The time which the synchronization happened.
	 * @param currentTimeOffset The time offset after the synchronization.
	 * @param change The change in time offset caused by the synchronization.
	 */
	public TimeSynchronizationResult(final TimeInstant timeStamp, final TimeOffset currentTimeOffset, final TimeOffset change) {
		this.timeStamp = timeStamp;
		this.currentTimeOffset = currentTimeOffset;
		this.change = change;
	}

	/**
	 * Deserializes a communication time stamps object.
	 *
	 * @param deserializer The deserializer.
	 */
	public TimeSynchronizationResult(final Deserializer deserializer) {
		this.timeStamp = UnixTime.fromDateString(deserializer.readString("dateTime"), new TimeInstant(0)).getTimeInstant();
		this.currentTimeOffset = TimeOffset.readFrom(deserializer, "currentTimeOffset");
		this.change = TimeOffset.readFrom(deserializer, "change");
	}

	/**
	 * Gets the time stamp.
	 *
	 * @return The time stamp.
	 */
	public TimeInstant getTimeStamp() {
		return this.timeStamp;
	}

	/**
	 * Gets the current time offset used to compute the network.
	 *
	 * @return The current time offset.
	 */
	public TimeOffset getCurrentTimeOffset() {
		return this.currentTimeOffset;
	}

	/**
	 * Gets the change in time offset caused by the synchronization.
	 *
	 * @return The change.
	 */
	public TimeOffset getChange() {
		return this.change;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeString("dateTime", UnixTime.fromTimeInstant(this.timeStamp).getDateString());
		TimeOffset.writeTo(serializer, "currentTimeOffset", this.currentTimeOffset);
		TimeOffset.writeTo(serializer, "change", this.change);
	}
}
