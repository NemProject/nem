package org.nem.nis.time.synchronization;

/**
 * A synchronization exception.
 */
public class SynchronizationException extends RuntimeException {

	/**
	 * Creates a new synchronization exception.
	 *
	 * @param message The exception message.
	 */
	public SynchronizationException(final String message) {
		super(message);
	}

	/**
	 * Creates a new synchronization exception.
	 *
	 * @param message The exception message.
	 * @param cause The original exception.
	 */
	public SynchronizationException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
