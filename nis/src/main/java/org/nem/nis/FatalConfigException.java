package org.nem.nis;

/**
 * A fatal configuration exception.
 */
@SuppressWarnings("serial")
public class FatalConfigException extends RuntimeException {

	/**
	 * Creates a new config exception.
	 *
	 * @param message The exception message.
	 */
	public FatalConfigException(final String message) {
		super(message);
	}

	/**
	 * Creates a new config exception.
	 *
	 * @param message The exception message.
	 * @param cause The original exception.
	 */
	public FatalConfigException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
