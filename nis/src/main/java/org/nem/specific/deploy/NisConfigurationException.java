package org.nem.specific.deploy;

/**
 * Exception that is used when a NIS configuration is corrupt.
 */
@SuppressWarnings("serial")
public class NisConfigurationException extends RuntimeException {

	/**
	 * Creates a new NIS configuration exception.
	 *
	 * @param message The exception message.
	 */
	public NisConfigurationException(final String message) {
		super(message);
	}

	/**
	 * Creates a new NIS configuration exception.
	 *
	 * @param cause The original exception.
	 */
	public NisConfigurationException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new NIS configuration exception.
	 *
	 * @param message The exception message.
	 * @param cause The original exception.
	 */
	public NisConfigurationException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
