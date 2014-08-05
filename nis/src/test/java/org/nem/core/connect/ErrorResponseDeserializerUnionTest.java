package org.nem.core.connect;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class ErrorResponseDeserializerUnionTest {

	@Test
	public void canCreateAroundErrorResponse() {
		// Arrange:
		final ErrorResponseDeserializerUnion union = new ErrorResponseDeserializerUnion(
				500,
				JsonSerializer.serializeToJson(new ErrorResponse(new TimeInstant(7), "badness", 700)),
				null);

		// Assert:
		Assert.assertThat(union.hasBody(), IsEqual.equalTo(true));
		Assert.assertThat(union.hasError(), IsEqual.equalTo(true));
		Assert.assertThat(500, IsEqual.equalTo(union.getStatus()));
		ExceptionAssert.assertThrows(v -> union.getDeserializer(), IllegalStateException.class);

		final ErrorResponse response = union.getError();
		Assert.assertThat(response.getMessage(), IsEqual.equalTo("badness"));
		Assert.assertThat(response.getStatus(), IsEqual.equalTo(700));
	}

	@Test
	public void canCreateAroundDeserializer() {
		// Arrange:
		final DeserializationContext context = new DeserializationContext(null);
		final ErrorResponseDeserializerUnion union = new ErrorResponseDeserializerUnion(
				200,
				JsonSerializer.serializeToJson(new MockSerializableEntity(2, "foo", 12)),
				context);

		// Assert:
		Assert.assertThat(union.hasBody(), IsEqual.equalTo(true));
		Assert.assertThat(union.hasError(), IsEqual.equalTo(false));
		Assert.assertThat(200, IsEqual.equalTo(union.getStatus()));
		ExceptionAssert.assertThrows(v -> union.getError(), IllegalStateException.class);

		final Deserializer deserializer = union.getDeserializer();
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);
		Assert.assertThat(deserializer.getContext(), IsSame.sameInstance(context));
		Assert.assertThat(entity, IsEqual.equalTo(new MockSerializableEntity(2, "foo", 12)));
	}

	@Test
	public void canCreateAroundEmptyStringError() {
		// Arrange:
		final ErrorResponseDeserializerUnion union = new ErrorResponseDeserializerUnion(500, "", null);

		// Assert:
		Assert.assertThat(union.hasBody(), IsEqual.equalTo(false));
		Assert.assertThat(union.hasError(), IsEqual.equalTo(true));
		Assert.assertThat(500, IsEqual.equalTo(union.getStatus()));
		ExceptionAssert.assertThrows(v -> union.getDeserializer(), IllegalStateException.class);
		ExceptionAssert.assertThrows(v -> union.getError(), IllegalStateException.class);
	}

	@Test
	public void canCreateAroundEmptyStringSuccess() {
		// Arrange:
		final ErrorResponseDeserializerUnion union = new ErrorResponseDeserializerUnion(200, "", null);

		// Assert:
		Assert.assertThat(union.hasBody(), IsEqual.equalTo(false));
		Assert.assertThat(union.hasError(), IsEqual.equalTo(false));
		Assert.assertThat(200, IsEqual.equalTo(union.getStatus()));
		ExceptionAssert.assertThrows(v -> union.getDeserializer(), IllegalStateException.class);
		ExceptionAssert.assertThrows(v -> union.getError(), IllegalStateException.class);
	}

	@Test
	public void canCreateAroundNonEmptyStringSuccess() {
		// Arrange:
		final ErrorResponseDeserializerUnion union = new ErrorResponseDeserializerUnion(200, "foo", null);

		// Assert:
		Assert.assertThat(union.hasBody(), IsEqual.equalTo(true));
		Assert.assertThat(union.hasError(), IsEqual.equalTo(false));
		Assert.assertThat(200, IsEqual.equalTo(union.getStatus()));
		ExceptionAssert.assertThrows(v -> union.getDeserializer(), IllegalStateException.class);
		ExceptionAssert.assertThrows(v -> union.getError(), IllegalStateException.class);
	}
}