package org.nem.core.crypto;

/**
 * Exception that is used when a cryptographic operation fails.
 */
public class CryptoException extends RuntimeException {

	/**
	 * Creates a new crypto exception.
	 *
	 * @param message The exception message.
	 */
	public CryptoException(final String message) {
		super(message);
	}

	/**
	 * Creates a new crypto exception.
	 *
	 * @param cause The exception cause.
	 */
	public CryptoException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new crypto exception.
	 *
	 * @param message The exception message.
	 * @param cause The exception cause.
	 */
	public CryptoException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
