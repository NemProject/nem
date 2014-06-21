package org.nem.nis.controller;

import org.nem.core.connect.ErrorResponse;
import org.nem.nis.controller.interceptors.UnauthorizedAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.MissingResourceException;

/**
 * ControllerAdvice-annotated class that maps thrown exceptions to JSON errors.
 */
@ResponseBody
@ControllerAdvice
public class ExceptionControllerAdvice {

	/**
	 * Handler for resource-not-found exceptions.
	 *
	 * @param e The exception.
	 * @return The appropriate response entity.
	 */
	@ExceptionHandler(MissingResourceException.class)
	public ResponseEntity<ErrorResponse> handleMissingResourceException(final Exception e) {
		return createResponse(e, HttpStatus.NOT_FOUND);
	}

	/**
	 * Handler for invalid-parameter exceptions.
	 *
	 * @param e The exception.
	 * @return The appropriate JSON indicating an error.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(final Exception e) {
		return createResponse(e, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handler for unauthorized-access exceptions.
	 *
	 * @param e The exception.
	 * @return The appropriate JSON indicating an error.
	 */
	@ExceptionHandler(UnauthorizedAccessException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(final Exception e) {
		return createResponse(e, HttpStatus.UNAUTHORIZED);
	}

	/**
	 * Handler for general exceptions.
	 *
	 * @param e The exception.
	 * @return The appropriate JSON indicating an error.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(final Exception e) {
		return createResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private static ResponseEntity<ErrorResponse> createResponse(final Exception e, final HttpStatus status) {
		return new ResponseEntity<>(new ErrorResponse(e, status), status);
	}
}
