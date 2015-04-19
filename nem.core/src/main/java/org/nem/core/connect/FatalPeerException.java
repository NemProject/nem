package org.nem.core.connect;

/**
 * A fatal (non-recoverable) peer exception.
 */
public class FatalPeerException extends RuntimeException {

	/**
	 * Creates a new exception.
	 *
	 * @param message The exception message.
	 */
	public FatalPeerException(final String message) {
		super(message);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param cause The exception message.
	 */
	public FatalPeerException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message The exception message.
	 * @param cause The original exception.
	 */
	public FatalPeerException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
