package org.nem.peer.node;

/**
 * An exception that is thrown when one peer is detected impersonating another.
 */
@SuppressWarnings("serial")
public class ImpersonatingPeerException extends RuntimeException {

	/**
	 * Creates a new exception.
	 *
	 * @param message The exception message.
	 */
	public ImpersonatingPeerException(final String message) {
		super(message);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param cause The exception message.
	 */
	public ImpersonatingPeerException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message The exception message.
	 * @param cause The original exception.
	 */
	public ImpersonatingPeerException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
