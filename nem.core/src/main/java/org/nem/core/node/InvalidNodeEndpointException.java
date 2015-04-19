package org.nem.core.node;

/**
 * Exception that is used when an invalid NodeEndpoint is attempted to be created.
 */
public class InvalidNodeEndpointException extends RuntimeException {

	/**
	 * Creates a new invalid node endpoint exception.
	 *
	 * @param message The exception message.
	 * @param cause The original exception.
	 */
	public InvalidNodeEndpointException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
