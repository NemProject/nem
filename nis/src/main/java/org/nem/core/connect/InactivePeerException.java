package org.nem.core.connect;

/**
 * An exception that is thrown when a connection cannot be established with a peer.
 */
public class InactivePeerException extends RuntimeException {

	/**
	 * Creates a new exception.
	 *
	 * @param message The exception message.
	 */
	public InactivePeerException(final String message) {
		super(message);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param cause The exception message.
	 */
	public InactivePeerException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message The exception message.
	 * @param cause The original exception.
	 */
	public InactivePeerException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
