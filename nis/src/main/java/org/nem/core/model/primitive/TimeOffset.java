package org.nem.core.model.primitive;

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
}
