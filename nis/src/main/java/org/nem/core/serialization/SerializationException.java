package org.nem.core.serialization;

/**
 * Exception that is used when a serialization operation fails.
 */
public class SerializationException extends RuntimeException {

	/**
	 * Creates a new serialization exception.
	 *
	 * @param message The exception message.
	 */
	public SerializationException(final String message) {
		super(message);
	}

	/**
	 * Creates a new serialization exception.
	 *
	 * @param cause The exception message.
	 */
	public SerializationException(final Throwable cause) {
		super(cause);
	}
}
