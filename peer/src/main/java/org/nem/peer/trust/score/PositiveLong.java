package org.nem.peer.trust.score;

/**
 * Simple class that wraps a long and constrains it to a positive value.
 */
public class PositiveLong {

	private long value;

	/**
	 * Creates a new positive long.
	 *
	 * @param value The long value.
	 */
	public PositiveLong(final long value) {
		this.set(value);
	}

	/**
	 * Gets the long value.
	 *
	 * @return The long value
	 */
	public long get() {
		return this.value;
	}

	/**
	 * Sets the long value.
	 *
	 * @param value The long value
	 */
	public void set(final long value) {
		this.value = Math.max(value, 0);
	}

	/**
	 * Increments the long value.
	 */
	public void increment() {
		++this.value;
	}
}
