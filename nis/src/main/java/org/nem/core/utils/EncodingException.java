package org.nem.core.utils;

/**
 * Exception that is used when an encoding operation fails.
 */
public class EncodingException extends RuntimeException {

	/**
	 * Creates a new encoding exception.
	 */
	public EncodingException() {
	}

	/**
	 * Creates a new encoding exception.
	 *
	 * @param cause The original exception.
	 */
	public EncodingException(final Throwable cause) {
		super(cause);
	}
}
