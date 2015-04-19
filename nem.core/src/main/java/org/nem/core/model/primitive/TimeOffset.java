package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

/**
 * Represents a time offset in ms.
 */
public class TimeOffset extends AbstractPrimitive<TimeOffset, Long> {

	/**
	 * Creates a time offset.
	 *
	 * @param offset The time offset.
	 */
	public TimeOffset(final long offset) {
		super(offset, TimeOffset.class);
	}

	/**
	 * Returns the time offset.
	 *
	 * @return The underlying time offset.
	 */
	public Long getRaw() {
		return this.getValue();
	}

	/**
	 * Adds a time offset to another time offset.
	 *
	 * @param offset The other time offset.
	 * @return The resulting time offset.
	 */
	public TimeOffset add(final TimeOffset offset) {
		return new TimeOffset(this.getRaw() + offset.getRaw());
	}

	/**
	 * Subtracts a time offset from another time offset.
	 *
	 * @param offset The other time offset.
	 * @return The resulting time offset.
	 */
	public TimeOffset subtract(final TimeOffset offset) {
		return new TimeOffset(this.getRaw() - offset.getRaw());
	}

	/**
	 * Writes a time offset object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param timeOffset time offset object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final TimeOffset timeOffset) {
		serializer.writeLong(label, timeOffset.getRaw());
	}

	/**
	 * Reads a time offset object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static TimeOffset readFrom(final Deserializer deserializer, final String label) {
		return new TimeOffset(deserializer.readLong(label));
	}
}
