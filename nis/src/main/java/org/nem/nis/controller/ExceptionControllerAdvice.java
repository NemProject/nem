package org.nem.nis.controller;

import org.nem.core.serialization.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidParameterException;
import java.util.MissingResourceException;
import java.util.logging.*;

/**
 * ControllerAdvice-annotated class that maps thrown exceptions to JSON errors.
 */
@ResponseBody
@ControllerAdvice
public class ExceptionControllerAdvice {

	private static final Logger LOGGER = Logger.getLogger(ExceptionControllerAdvice.class.getName());

	/**
	 * Handler for resource-not-found exceptions.
	 *
	 * @param e The exception.
	 *
	 * @return The appropriate JSON indicating an error.
	 */
	@ExceptionHandler(MissingResourceException.class)
	public Error handleMissingResourceException(final Exception e) {
		return getErrorString(e, HttpStatus.NOT_FOUND);
	}

	/**
	 * Handler for invalid-parameter exceptions.
	 *
	 * @param e The exception.
	 *
	 * @return The appropriate JSON indicating an error.
	 */
	@ExceptionHandler(InvalidParameterException.class)
	public Error handleInvalidParameterException(final Exception e) {
		return getErrorString(e, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handler for general exceptions.
	 *
	 * @param e The exception.
	 *
	 * @return The appropriate JSON indicating an error.
	 */
	@ExceptionHandler(Exception.class)
	public Error handleException(final Exception e) {
		return getErrorString(e, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	// TODO: add a few tests

	// TODO: unify with Utils.jsonError
	private static Error getErrorString(final Exception e, final HttpStatus status) {
		Level logLevel = HttpStatus.INTERNAL_SERVER_ERROR == status ? Level.SEVERE : Level.INFO;
		LOGGER.log(logLevel, e.getMessage());
		return new Error(status);
	}

	// TODO: rename and promote
	public static class Error implements SerializableEntity {

		final HttpStatus status;

		public Error(final HttpStatus status) {
			this.status = status;
		}

		@Override
		public void serialize(final Serializer serializer) {
			serializer.writeInt("status", status.value());
			serializer.writeString("error", status.getReasonPhrase());
		}
	}
}
