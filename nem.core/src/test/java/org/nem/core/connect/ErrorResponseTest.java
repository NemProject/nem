package org.nem.core.connect;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.HttpStatus;

public class ErrorResponseTest {

	@Test
	public void canBeCreatedAroundException() {
		// Arrange:
		final ErrorResponse response = new ErrorResponse(new TimeInstant(18), new RuntimeException("exception message"),
				HttpStatus.NOT_FOUND);

		// Assert:
		MatcherAssert.assertThat(response.getTimeStamp(), IsEqual.equalTo(new TimeInstant(18)));
		MatcherAssert.assertThat(response.getError(), IsEqual.equalTo("Not Found"));
		MatcherAssert.assertThat(response.getMessage(), IsEqual.equalTo("exception message"));
		MatcherAssert.assertThat(response.getStatus(), IsEqual.equalTo(404));
	}

	@Test
	public void canBeCreatedAroundMessage() {
		// Arrange:
		final ErrorResponse response = new ErrorResponse(new TimeInstant(29), "badness", 500);

		// Assert:
		MatcherAssert.assertThat(response.getTimeStamp(), IsEqual.equalTo(new TimeInstant(29)));
		MatcherAssert.assertThat(response.getError(), IsEqual.equalTo("Internal Server Error"));
		MatcherAssert.assertThat(response.getMessage(), IsEqual.equalTo("badness"));
		MatcherAssert.assertThat(response.getStatus(), IsEqual.equalTo(500));
	}

	@Test
	public void canBeCreatedAroundUnknownHttpStatus() {
		// Arrange:
		final ErrorResponse response = new ErrorResponse(new TimeInstant(18), "exception message", -123);

		// Assert:
		MatcherAssert.assertThat(response.getTimeStamp(), IsEqual.equalTo(new TimeInstant(18)));
		MatcherAssert.assertThat(response.getError(), IsNull.nullValue());
		MatcherAssert.assertThat(response.getMessage(), IsEqual.equalTo("exception message"));
		MatcherAssert.assertThat(response.getStatus(), IsEqual.equalTo(-123));
	}

	@Test
	public void responseCanBeSerialized() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final ErrorResponse response = new ErrorResponse(new TimeInstant(18), "badness", 500);

		// Act:
		response.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();

		// Assert:
		MatcherAssert.assertThat(jsonObject.get("timeStamp"), IsEqual.equalTo(18));
		MatcherAssert.assertThat(jsonObject.get("error"), IsEqual.equalTo("Internal Server Error"));
		MatcherAssert.assertThat(jsonObject.get("message"), IsEqual.equalTo("badness"));
		MatcherAssert.assertThat(jsonObject.get("status"), IsEqual.equalTo(500));
	}

	@Test
	public void responseCanBeRoundTripped() {
		// Act:
		final ErrorResponse response = createRoundTrippedResponse(new ErrorResponse(new TimeInstant(18), "badness", 500));

		// Assert:
		MatcherAssert.assertThat(response.getTimeStamp(), IsEqual.equalTo(new TimeInstant(18)));
		MatcherAssert.assertThat(response.getError(), IsEqual.equalTo("Internal Server Error"));
		MatcherAssert.assertThat(response.getMessage(), IsEqual.equalTo("badness"));
		MatcherAssert.assertThat(response.getStatus(), IsEqual.equalTo(500));
	}

	@Test
	public void responseWithoutDescriptionsCanBeRoundTripped() {
		// Act:
		final ErrorResponse response = createRoundTrippedResponse(new ErrorResponse(new TimeInstant(54), null, 890));

		// Assert:
		MatcherAssert.assertThat(response.getTimeStamp(), IsEqual.equalTo(new TimeInstant(54)));
		MatcherAssert.assertThat(response.getError(), IsNull.nullValue());
		MatcherAssert.assertThat(response.getMessage(), IsNull.nullValue());
		MatcherAssert.assertThat(response.getStatus(), IsEqual.equalTo(890));
	}

	private static ErrorResponse createRoundTrippedResponse(final ErrorResponse originalResponse) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalResponse, null);
		return new ErrorResponse(deserializer);
	}

	@Test
	public void toStringReturnsCorrectRepresentationWhenStatusCodeIsKnown() {
		// Assert:
		MatcherAssert.assertThat("Http Status Code 404: badness",
				IsEqual.equalTo(new ErrorResponse(new TimeInstant(4), "badness", 404).toString()));
		MatcherAssert.assertThat("Http Status Code 404: Not Found",
				IsEqual.equalTo(new ErrorResponse(new TimeInstant(4), null, 404).toString()));
	}

	@Test
	public void toStringReturnsCorrectRepresentationWhenStatusCodeIsUnknown() {
		// Assert:
		MatcherAssert.assertThat("Http Status Code -123: badness",
				IsEqual.equalTo(new ErrorResponse(new TimeInstant(4), "badness", -123).toString()));
		MatcherAssert.assertThat("Http Status Code -123", IsEqual.equalTo(new ErrorResponse(new TimeInstant(4), null, -123).toString()));
	}
}
