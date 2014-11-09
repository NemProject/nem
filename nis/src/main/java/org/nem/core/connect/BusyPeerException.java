package org.nem.core.connect;

/**
 * An exception that is thrown when peer communication fails because a peer is busy.
 */
public class BusyPeerException extends RuntimeException {

	/**
	 * Creates a new exception.
	 *
	 * @param message The exception message.
	 */
	public BusyPeerException(final String message) {
		super(message);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param cause The exception message.
	 */
	public BusyPeerException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message The exception message.
	 * @param cause The original exception.
	 */
	public BusyPeerException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
