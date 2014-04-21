package org.nem.nis.controller.utils;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.springframework.http.HttpStatus;

public class ErrorResponseTest {

	@Test
	public void canBeCreatedAroundException() {
		// Arrange:
		final ErrorResponse response = new ErrorResponse(
				new RuntimeException("exception message"),
				HttpStatus.NOT_FOUND);

		// Assert:
		Assert.assertThat(response.getTimeStamp(), IsNot.not(IsEqual.equalTo(0)));
		Assert.assertThat(response.getError(), IsEqual.equalTo("Not Found"));
		Assert.assertThat(response.getMessage(), IsEqual.equalTo("exception message"));
		Assert.assertThat(response.getStatus(), IsEqual.equalTo(404));
	}

	@Test
	public void canBeCreatedAroundMessage() {
		// Arrange:
		final ErrorResponse response = new ErrorResponse("badness", 500);

		// Assert:
		Assert.assertThat(response.getTimeStamp(), IsNot.not(IsEqual.equalTo(0)));
		Assert.assertThat(response.getError(), IsEqual.equalTo("Internal Server Error"));
		Assert.assertThat(response.getMessage(), IsEqual.equalTo("badness"));
		Assert.assertThat(response.getStatus(), IsEqual.equalTo(500));
	}

	@Test
	public void canBeCreatedAroundUnknownHttpStatus() {
		// Arrange:
		final ErrorResponse response = new ErrorResponse("exception message", -123);

		// Assert:
		Assert.assertThat(response.getTimeStamp(), IsNot.not(IsEqual.equalTo(0)));
		Assert.assertThat(response.getError(), IsEqual.equalTo(null));
		Assert.assertThat(response.getMessage(), IsEqual.equalTo("exception message"));
		Assert.assertThat(response.getStatus(), IsEqual.equalTo(-123));
	}

	@Test
	public void responseCanBeSerialized() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final ErrorResponse response = new ErrorResponse("badness", 500);

		// Act:
		response.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();

		// Assert:
		Assert.assertThat((Integer)jsonObject.get("timeStamp"), IsNot.not(IsEqual.equalTo(0)));
		Assert.assertThat((String)jsonObject.get("error"), IsEqual.equalTo("Internal Server Error"));
		Assert.assertThat((String)jsonObject.get("message"), IsEqual.equalTo("badness"));
		Assert.assertThat((Integer)jsonObject.get("status"), IsEqual.equalTo(500));
	}

	@Test
	public void responseCanBeRoundTripped() {
		// Act:
		final ErrorResponse response = createRoundTrippedResponse(new ErrorResponse("badness", 500));

		// Assert:
		Assert.assertThat(response.getTimeStamp(), IsNot.not(IsEqual.equalTo(0)));
		Assert.assertThat(response.getError(), IsEqual.equalTo("Internal Server Error"));
		Assert.assertThat(response.getMessage(), IsEqual.equalTo("badness"));
		Assert.assertThat(response.getStatus(), IsEqual.equalTo(500));
	}

	private static ErrorResponse createRoundTrippedResponse(final ErrorResponse originalResponse) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalResponse, null);
		return new ErrorResponse(deserializer);
	}

	@Test
	public void timeStampIsPopulatedWithCurrentSystemTime() {
		// Act:
		final TimeProvider timeProvider = new SystemTimeProvider();
		ErrorResponse response;
		int systemTime;
		int systemTimeEnd;
		do {
			systemTime = timeProvider.getCurrentTime().getRawTime();

			response = new ErrorResponse("badness", 500);

			systemTimeEnd = timeProvider.getCurrentTime().getRawTime();

		} while (systemTime != systemTimeEnd);

		// Assert:
		Assert.assertThat(response.getTimeStamp(), IsEqual.equalTo(systemTime));
	}

	@Test
	public void toStringReturnsCorrectRepresentationWhenStatusCodeIsKnown() {
		// Assert:
		Assert.assertThat(
				"Http Status Code 404: badness",
				IsEqual.equalTo(new ErrorResponse("badness", 404).toString()));
		Assert.assertThat(
				"Http Status Code 404: Not Found",
				IsEqual.equalTo(new ErrorResponse(null, 404).toString()));
	}

	@Test
	public void toStringReturnsCorrectRepresentationWhenStatusCodeIsUnknown() {
		// Assert:
		Assert.assertThat(
				"Http Status Code -123: badness",
				IsEqual.equalTo(new ErrorResponse("badness", -123).toString()));
		Assert.assertThat(
				"Http Status Code -123",
				IsEqual.equalTo(new ErrorResponse(null, -123).toString()));
	}
}
