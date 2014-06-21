package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.connect.ErrorResponse;
import org.springframework.http.*;

public class ExceptionControllerAdviceTest {

	@Test
	public void handleMissingResourceExceptionCreatesAppropriateResponse() {
		// Arrange:
		final ExceptionControllerAdvice advice = new ExceptionControllerAdvice();
		final ResponseEntity<ErrorResponse> entity = advice.handleMissingResourceException(new Exception("badness"));

		// Assert:
		Assert.assertThat(entity.getStatusCode(), IsEqual.equalTo(HttpStatus.NOT_FOUND));
		Assert.assertThat(entity.getBody().getStatus(), IsEqual.equalTo(HttpStatus.NOT_FOUND.value()));
		Assert.assertThat(entity.getBody().getMessage(), IsEqual.equalTo("badness"));
	}

	@Test
	public void handleIllegalArgumentExceptionCreatesAppropriateResponse() {
		// Arrange:
		final ExceptionControllerAdvice advice = new ExceptionControllerAdvice();
		final ResponseEntity<ErrorResponse> entity = advice.handleIllegalArgumentException(new Exception("badness"));

		// Assert:
		Assert.assertThat(entity.getStatusCode(), IsEqual.equalTo(HttpStatus.BAD_REQUEST));
		Assert.assertThat(entity.getBody().getStatus(), IsEqual.equalTo(HttpStatus.BAD_REQUEST.value()));
		Assert.assertThat(entity.getBody().getMessage(), IsEqual.equalTo("badness"));
	}

	@Test
	public void handleUnauthorizedAccessExceptionCreatesAppropriateResponse() {
		// Arrange:
		final ExceptionControllerAdvice advice = new ExceptionControllerAdvice();
		final ResponseEntity<ErrorResponse> entity = advice.handleUnauthorizedAccessException(new Exception("badness"));

		// Assert:
		Assert.assertThat(entity.getStatusCode(), IsEqual.equalTo(HttpStatus.UNAUTHORIZED));
		Assert.assertThat(entity.getBody().getStatus(), IsEqual.equalTo(HttpStatus.UNAUTHORIZED.value()));
		Assert.assertThat(entity.getBody().getMessage(), IsEqual.equalTo("badness"));
	}

	@Test
	public void handleExceptionCreatesAppropriateResponse() {
		// Arrange:
		final ExceptionControllerAdvice advice = new ExceptionControllerAdvice();
		final ResponseEntity<ErrorResponse> entity = advice.handleException(new Exception("badness"));

		// Assert:
		Assert.assertThat(entity.getStatusCode(), IsEqual.equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
		Assert.assertThat(entity.getBody().getStatus(), IsEqual.equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value()));
		Assert.assertThat(entity.getBody().getMessage(), IsEqual.equalTo("badness"));
	}
}
