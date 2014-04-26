package org.nem.core.connect;

import org.apache.http.*;
import org.apache.http.client.methods.HttpRequestBase;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.io.*;

public class HttpJsonResponseStrategyTest {

	//region HttpJsonResponseStrategy

	@Test(expected = InactivePeerException.class)
	public void coerceThrowsInactivePeerExceptionOnHttpError() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = new HttpDeserializerResponseStrategy(null);
		final HttpResponse response = Mockito.mock(HttpResponse.class);
		mockStatusCode(response, 500);

		// Act:
		strategy.coerce(Mockito.mock(HttpRequestBase.class), response);
	}

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionOnIoError() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = new HttpDeserializerResponseStrategy(null);
		final HttpResponse response = Mockito.mock(HttpResponse.class);
		mockStatusCode(response, 200);
		Mockito.when(response.getEntity()).thenThrow(new IOException());

		// Act:
		strategy.coerce(Mockito.mock(HttpRequestBase.class), response);
	}

	//endregion

	//region HttpDeserializerResponseStrategy

	@Test
	public void coercedDeserializerIsCorrectlyCreatedAroundInput() throws Exception {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		final Deserializer deserializer = coerceDeserializer(originalEntity, new MockAccountLookup());
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		Assert.assertThat(entity, IsEqual.equalTo(originalEntity));
	}

	@Test
	public void coercedDeserializerIsAssociatedWithAccountLookup() throws Exception {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		final Deserializer deserializer = coerceDeserializer(originalEntity, accountLookup);
		deserializer.getContext().findAccountByAddress(Address.fromEncoded("foo"));

		// Assert:
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionIfPeerReturnsUnexpectedDataWhenDeserializerIsExpected() throws Exception {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();

		// Act:
		coerceDeserializer(new byte[] { }, accountLookup);
	}

	//endregion

	//region HttpVoidResponseStrategy

	@Test
	public void nullIsReturnedIfRequestSucceedsAndNoDataIsReturned() throws Exception {
		// Arrange:
		final HttpVoidResponseStrategy strategy = new HttpVoidResponseStrategy();

		// Act:
		final Deserializer deserializer = coerceDeserializer(new byte[] { }, strategy);

		// Assert:
		Assert.assertThat(deserializer, IsEqual.equalTo(null));
	}

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionIfPeerReturnsDataWhenNoneIsExpected() throws Exception {
		// Arrange:
		final HttpVoidResponseStrategy strategy = new HttpVoidResponseStrategy();

		// Act:
		coerceDeserializer("some data".getBytes(), strategy);
	}

	//endregion

	private static Deserializer coerceDeserializer(
			final byte[] serializedBytes,
			final HttpJsonResponseStrategy<Deserializer> strategy) throws IOException {
		// Arrange:
		final HttpResponse response = Mockito.mock(HttpResponse.class);
		mockStatusCode(response, 200);

		final HttpEntity entity = Mockito.mock(HttpEntity.class);
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedBytes);
		Mockito.when(response.getEntity()).thenReturn(entity);
		Mockito.when(entity.getContent()).thenReturn(inputStream);

		// Act:
		return strategy.coerce(Mockito.mock(HttpRequestBase.class), response);
	}

	private static void mockStatusCode(final HttpResponse response, final int statusCode) {
		// Arrange:
		final StatusLine statusLine = Mockito.mock(StatusLine.class);
		Mockito.when(response.getStatusLine()).thenReturn(statusLine);
		Mockito.when(statusLine.getStatusCode()).thenReturn(statusCode);
	}

	private static Deserializer coerceDeserializer(
			final byte[] serializedBytes,
			final AccountLookup accountLookup) throws IOException {
		// Arrange:
		final DeserializationContext context = new DeserializationContext(accountLookup);
		final HttpDeserializerResponseStrategy strategy = new HttpDeserializerResponseStrategy(context);

		// Act:
		return coerceDeserializer(serializedBytes, strategy);
	}

	private static Deserializer coerceDeserializer(
			final SerializableEntity originalEntity,
			final AccountLookup accountLookup) throws IOException {
		// Arrange:
		final byte[] serializedBytes = JsonSerializer.serializeToJson(originalEntity).toJSONString().getBytes();

		// Act:
		return coerceDeserializer(serializedBytes, accountLookup);
	}
}
