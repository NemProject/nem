package org.nem.core.model;

/**
 * Possible NEM statuses.
 *
 * TODO 20140909: J-B just a comment - should these be bit flags so a node could be running + synched or running + booted + synced
 */
public enum NemStatus {
	/**
	 * Unknown status.
	 */
	UNKNOWN(0),

	/**
	 * NIS/NCC is stopped.
	 */
	STOPPED(1),

	/**
	 * NIS/NCC is starting.
	 */
	STARTING(2),

	/**
	 * NIS/NCC is running.
	 */
	RUNNING(3),

	/**
	 * Local node is booted (implies RUNNING).
	 */
	BOOTED(4),

	/**
	 * NIS local node is synchronized (implies RUNNING and BOOTED)
	 */
	SYNCHRONIZED(5);

	private final int value;

	private NemStatus(final int value) {
		this.value = value;
	}

	/**
	 * Creates a NEM status given a raw value.
	 *
	 * TODO 20140909: J-B add a test :)
	 *
	 * @param value The value.
	 * @return The NEM status if the value is known.
	 * @throws IllegalArgumentException if the value is unknown.
	 */
	public static NemStatus fromValue(final int value) {
		for (final NemStatus result : values()) {
			if (result.getValue() == value) {
				return result;
			}
		}

		throw new IllegalArgumentException("Invalid NEM status: " + value);
	}

	/**
	 * Gets the underlying integer representation of the status.
	 *
	 * @return The underlying value.
	 */
	public int getValue() {
		return this.value;
	}
}
