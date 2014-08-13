package org.nem.core.time;

import org.nem.core.serialization.*;

/**
 * Represents an instant in time.
 */
public class TimeInstant implements Comparable<TimeInstant> {

	private final int time;

	/**
	 * TimeInstant representing time 0.
	 */
	public static final TimeInstant ZERO = new TimeInstant(0);

	/**
	 * Creates an instant in time.
	 *
	 * @param time The number of seconds passed since the epoch.
	 */
	public TimeInstant(final int time) {
		if (time < 0) {
			throw new IllegalArgumentException("time must be non-negative");
		}

		this.time = time;
	}

	/**
	 * Creates a new TimeInstant by adding the specified number of seconds to this instant.
	 *
	 * @param seconds The number of seconds to add.
	 * @return The new instant.
	 */
	public TimeInstant addSeconds(final int seconds) {
		return new TimeInstant(this.time + seconds);
	}

	/**
	 * Creates a new TimeInstant by adding the specified number of minutes to this instant.
	 *
	 * @param minutes The number of minutes to add.
	 * @return The new instant.
	 */
	public TimeInstant addMinutes(final int minutes) {
		return this.addSeconds(60 * minutes);
	}

	/**
	 * Creates a new TimeInstant by adding the specified number of hours to this instant.
	 *
	 * @param hours The number of hours to add.
	 * @return The new instant.
	 */
	public TimeInstant addHours(final int hours) {
		return this.addMinutes(60 * hours);
	}

	/**
	 * Creates a new TimeInstant by adding the specified number of days to this instant.
	 *
	 * @param hours The number of days to add.
	 * @return The new instant.
	 */
	public TimeInstant addDays(final int hours) {
		return this.addHours(24 * hours);
	}

	/**
	 * Returns the number of seconds between this TimeInstant and rhs.
	 *
	 * @param rhs The value to subtract.
	 * @return The subtraction result in seconds.
	 */
	public int subtract(final TimeInstant rhs) {
		return this.time - rhs.time;
	}

	/**
	 * Compares this instant to another TimeInstant.
	 *
	 * @param rhs The instant to compare against.
	 * @return -1, 0 or 1 as this TimeInstant is numerically less than, equal to, or greater than rhs.
	 */
	@Override
	public int compareTo(final TimeInstant rhs) {
		return Integer.compare(this.time, rhs.time);
	}

	/**
	 * Returns the number of seconds passed since the epoch.
	 *
	 * @return The number of seconds passed since the epoch.
	 */
	public int getRawTime() {
		return this.time;
	}

	@Override
	public int hashCode() {
		return this.time;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof TimeInstant)) {
			return false;
		}

		final TimeInstant rhs = (TimeInstant)obj;
		return this.time == rhs.time;
	}

	@Override
	public String toString() {
		return String.format("%d", this.time);
	}

	//region inline serialization

	/**
	 * Writes a time instant object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param instant The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final TimeInstant instant) {
		serializer.writeInt(label, instant.getRawTime());
	}

	/**
	 * Reads a time instant object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static TimeInstant readFrom(final Deserializer deserializer, final String label) {
		return new TimeInstant(deserializer.readInt(label));
	}

	//endregion
}
