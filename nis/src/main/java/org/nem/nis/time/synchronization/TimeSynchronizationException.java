package org.nem.nis.time.synchronization;

/**
 * A synchronization exception.
 */
@SuppressWarnings("serial")
public class TimeSynchronizationException extends RuntimeException {

	/**
	 * Creates a new synchronization exception.
	 *
	 * @param message The exception message.
	 */
	public TimeSynchronizationException(final String message) {
		super(message);
	}

	/**
	 * Creates a new synchronization exception.
	 *
	 * @param message The exception message.
	 * @param cause The original exception.
	 */
	public TimeSynchronizationException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
