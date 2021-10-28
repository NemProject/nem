package org.nem.nis.mappers;

/**
 * Exception that is used when a mapping operation fails.
 */
@SuppressWarnings("serial")
public class MappingException extends RuntimeException {

	/**
	 * Creates a new mapping exception.
	 *
	 * @param message The exception message.
	 */
	public MappingException(final String message) {
		super(message);
	}

	/**
	 * Creates a new mapping exception.
	 *
	 * @param cause The original exception.
	 */
	public MappingException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new mapping exception.
	 *
	 * @param message The exception message.
	 * @param cause The original exception.
	 */
	public MappingException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
