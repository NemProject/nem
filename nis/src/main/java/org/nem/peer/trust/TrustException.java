package org.nem.peer.trust;

/**
 * A peer trust exception.
 */
public class TrustException extends RuntimeException {

	/**
	 * Creates a new exception.
	 *
	 * @param message The exception message.
	 */
	public TrustException(final String message) {
		super(message);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param cause The exception message.
	 */
	public TrustException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message The exception message.
	 * @param cause The original exception.
	 */
	public TrustException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
