package org.nem.core.connect;

import org.apache.http.*;
import org.apache.http.client.methods.HttpRequestBase;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.io.*;

public class HttpErrorResponseDeserializerUnionStrategyTest {

	@Test
	public void canCoerceDeserializer() throws IOException {
		// Arrange:
		final DeserializationContext context = new DeserializationContext(null);

		// Act:
		final ErrorResponseDeserializerUnion union = coerceUnion(
				200,
				JsonSerializer.serializeToBytes(new MockSerializableEntity(2, "foo", 12)),
				context);
		final MockSerializableEntity entity = new MockSerializableEntity(union.getDeserializer());

		// Assert:
		Assert.assertThat(union.getDeserializer().getContext(), IsSame.sameInstance(context));
		Assert.assertThat(entity, IsEqual.equalTo(new MockSerializableEntity(2, "foo", 12)));
	}

	@Test
	public void canCoerceErrorResponse() throws IOException {
		// Act:
		final ErrorResponseDeserializerUnion union = coerceUnion(
				404,
				JsonSerializer.serializeToBytes(new ErrorResponse(new TimeInstant(3), "badness", 700)),
				null);
		final ErrorResponse response = union.getError();

		// Assert:
		Assert.assertThat(response.getMessage(), IsEqual.equalTo("badness"));
		Assert.assertThat(response.getStatus(), IsEqual.equalTo(700));
	}

	@Test
	public void canCoerceEmptyString() throws IOException {
		// Act:
		final ErrorResponseDeserializerUnion union = coerceUnion(
				404,
				"".getBytes(),
				null);

		// Assert:
		Assert.assertThat(union.hasBody(), IsEqual.equalTo(false));
	}

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionOnIoError() throws IOException {
		// Arrange:
		final HttpErrorResponseDeserializerUnionStrategy strategy = new HttpErrorResponseDeserializerUnionStrategy(null);

		// Act:
		ConnectUtils.coerceStreamWithIoError(strategy);
	}

	private static ErrorResponseDeserializerUnion coerceUnion(
			final int statusCode,
			final byte[] serializedBytes,
			final DeserializationContext context) throws IOException {
		// Arrange:
		final HttpErrorResponseDeserializerUnionStrategy strategy = new HttpErrorResponseDeserializerUnionStrategy(context);

		final HttpResponse response = Mockito.mock(HttpResponse.class);
		ConnectUtils.mockStatusCode(response, statusCode);

		final HttpEntity entity = Mockito.mock(HttpEntity.class);
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedBytes);
		Mockito.when(response.getEntity()).thenReturn(entity);
		Mockito.when(entity.getContent()).thenReturn(inputStream);

		// Act:
		return strategy.coerce(Mockito.mock(HttpRequestBase.class), response);
	}
}