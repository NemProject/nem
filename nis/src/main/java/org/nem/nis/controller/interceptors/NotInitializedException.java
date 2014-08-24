package org.nem.nis.controller.interceptors;

/*
 * Exception that is thrown when NIS hasn't been initialized.
 */
public class NotInitializedException extends RuntimeException {

	/**
	 * Creates a new not initialized exception.
	 *
	 * @param message The exception message.
	 */
	public NotInitializedException(final String message) {
		super(message);
	}
}
