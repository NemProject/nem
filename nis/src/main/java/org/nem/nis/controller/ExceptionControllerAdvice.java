package org.nem.nis.controller;

import org.nem.core.connect.ErrorResponse;
import org.nem.core.time.TimeProvider;
import org.nem.nis.NisIllegalStateException;
import org.nem.nis.controller.interceptors.UnauthorizedAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.MissingResourceException;

/**
 * ControllerAdvice-annotated class that maps thrown exceptions to JSON errors.
 */
@ResponseBody
@ControllerAdvice
public class ExceptionControllerAdvice {
	private final TimeProvider timeProvider;

	/**
	 * Creates a new exception controller advice.
	 *
	 * @param timeProvider The time provider.
	 */
	@Autowired(required = true)
	public ExceptionControllerAdvice(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	/**
	 * Handler for resource-not-found exceptions.
	 *
	 * @param e The exception.
	 * @return The appropriate response entity.
	 */
	@ExceptionHandler(MissingResourceException.class)
	public ResponseEntity<ErrorResponse> handleMissingResourceException(final MissingResourceException e) {
		return this.createResponse(String.format("%s '%s' (%s)", e.getMessage(), e.getKey(), e.getClassName()), HttpStatus.NOT_FOUND);
	}

	/**
	 * Handler for invalid-parameter exceptions.
	 *
	 * @param e The exception.
	 * @return The appropriate JSON indicating an error.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(final Exception e) {
		return this.createResponse(e, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handler for unauthorized-access exceptions.
	 *
	 * @param e The exception.
	 * @return The appropriate JSON indicating an error.
	 */
	@ExceptionHandler(UnauthorizedAccessException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(final Exception e) {
		return this.createResponse(e, HttpStatus.UNAUTHORIZED);
	}

	/**
	 * Handler for NIS illegal state exceptions.
	 *
	 * @param e The exception.
	 * @return The appropriate JSON indicating an error.
	 */
	@ExceptionHandler(NisIllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleNisIllegalStateException(final NisIllegalStateException e) {
		return this.createResponse(e, HttpStatus.SERVICE_UNAVAILABLE);
	}

	/**
	 * Handler for general exceptions.
	 *
	 * @param e The exception.
	 * @return The appropriate JSON indicating an error.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(final Exception e) {
		return this.createResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private ResponseEntity<ErrorResponse> createResponse(final String message, final HttpStatus status) {
		return new ResponseEntity<>(new ErrorResponse(this.timeProvider.getCurrentTime(), message, status.value()), status);
	}

	private ResponseEntity<ErrorResponse> createResponse(final Exception e, final HttpStatus status) {
		return this.createResponse(e.getMessage(), status);
	}
}
