package org.nem.nis.controller;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.ErrorResponse;
import org.nem.core.time.*;
import org.nem.nis.NisIllegalStateException;
import org.springframework.http.*;

import java.util.MissingResourceException;

public class ExceptionControllerAdviceTest {
	private static final TimeInstant CURRENT_TIME = new TimeInstant(57);

	@Test
	public void handleMissingResourceExceptionCreatesAppropriateResponse() {
		// Arrange:
		final ExceptionControllerAdvice advice = createAdvice();
		final ResponseEntity<ErrorResponse> entity = advice
				.handleMissingResourceException(new MissingResourceException("badness", "class", "resource"));

		// Assert:
		assertEntity(entity, HttpStatus.NOT_FOUND, "badness 'resource' (class)");
	}

	@Test
	public void handleIllegalArgumentExceptionCreatesAppropriateResponse() {
		// Arrange:
		final ExceptionControllerAdvice advice = createAdvice();
		final ResponseEntity<ErrorResponse> entity = advice.handleIllegalArgumentException(new Exception("badness"));

		// Assert:
		assertEntity(entity, HttpStatus.BAD_REQUEST);
	}

	@Test
	public void handleUnauthorizedAccessExceptionCreatesAppropriateResponse() {
		// Arrange:
		final ExceptionControllerAdvice advice = createAdvice();
		final ResponseEntity<ErrorResponse> entity = advice.handleUnauthorizedAccessException(new Exception("badness"));

		// Assert:
		assertEntity(entity, HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void handleNisIllegalStateExceptionCreatesAppropriateResponse() {
		// Arrange:
		final ExceptionControllerAdvice advice = createAdvice();
		final ResponseEntity<ErrorResponse> entity = advice
				.handleNisIllegalStateException(new NisIllegalStateException(NisIllegalStateException.Reason.NIS_ILLEGAL_STATE_NOT_BOOTED));

		// Assert:
		assertEntity(entity, HttpStatus.SERVICE_UNAVAILABLE, "NIS_ILLEGAL_STATE_NOT_BOOTED");
	}

	@Test
	public void handleExceptionCreatesAppropriateResponse() {
		// Arrange:
		final ExceptionControllerAdvice advice = createAdvice();
		final ResponseEntity<ErrorResponse> entity = advice.handleException(new Exception("badness"));

		// Assert:
		assertEntity(entity, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private static void assertEntity(final ResponseEntity<ErrorResponse> entity, final HttpStatus expectedCode) {
		// Assert:
		assertEntity(entity, expectedCode, "badness");
	}

	private static void assertEntity(final ResponseEntity<ErrorResponse> entity, final HttpStatus expectedCode,
			final String expectedMessage) {
		// Assert:
		MatcherAssert.assertThat(entity.getStatusCode(), IsEqual.equalTo(expectedCode));
		MatcherAssert.assertThat(entity.getBody().getTimeStamp(), IsEqual.equalTo(CURRENT_TIME));
		MatcherAssert.assertThat(entity.getBody().getStatus(), IsEqual.equalTo(expectedCode.value()));
		MatcherAssert.assertThat(entity.getBody().getMessage(), IsEqual.equalTo(expectedMessage));
	}

	private static ExceptionControllerAdvice createAdvice() {
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(CURRENT_TIME);
		return new ExceptionControllerAdvice(timeProvider);
	}
}
