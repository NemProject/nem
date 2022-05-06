package org.nem.nis.controller.interceptors;

/**
 * Exception that is thrown when an unauthorized request is made.
 */
@SuppressWarnings("serial")
public class UnauthorizedAccessException extends RuntimeException {

	/**
	 * Creates a new unauthorized access exception.
	 *
	 * @param message The exception message.
	 */
	public UnauthorizedAccessException(final String message) {
		super(message);
	}
}
