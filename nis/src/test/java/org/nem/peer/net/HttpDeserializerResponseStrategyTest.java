package org.nem.peer.net;

import org.eclipse.jetty.client.api.*;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.peer.*;
import org.nem.peer.InactivePeerException;

import java.io.*;
import java.util.*;

public class HttpDeserializerResponseStrategyTest {

	@Test(expected = InactivePeerException.class)
	public void coerceThrowsInactivePeerExceptionOnHttpError() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = new HttpDeserializerResponseStrategy(null);
		final Response response = Mockito.mock(Response.class);
		Mockito.when(response.getStatus()).thenReturn(500);

		// Act:
		strategy.coerce(Mockito.mock(Request.class), response);
	}

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionIfThereAreTooFewListeners() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = new HttpDeserializerResponseStrategy(null);
		final Response response = Mockito.mock(Response.class);
		Mockito.when(response.getStatus()).thenReturn(200);

		final List<InputStreamResponseListener> listeners = new ArrayList<>();
		Mockito.when(response.getListeners(InputStreamResponseListener.class)).thenReturn(listeners);

		// Act:
		strategy.coerce(Mockito.mock(Request.class), response);
	}

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionIfThereAreTooManyListeners() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = new HttpDeserializerResponseStrategy(null);
		final Response response = Mockito.mock(Response.class);
		Mockito.when(response.getStatus()).thenReturn(200);

		final List<InputStreamResponseListener> listeners = new ArrayList<>();
		listeners.add(Mockito.mock(InputStreamResponseListener.class));
		listeners.add(Mockito.mock(InputStreamResponseListener.class));
		Mockito.when(response.getListeners(InputStreamResponseListener.class)).thenReturn(listeners);

		// Act:
		strategy.coerce(Mockito.mock(Request.class), response);
	}

	@Test
	public void coercedDeserializerIsCorrectlyCreatedAroundInput() throws Exception {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		final Deserializer deserializer = coerceDeserializer(originalEntity, new MockAccountLookup());
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		CustomAsserts.assertMockSerializableEntity(entity, 7, "foo", 3L);
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

	private static Deserializer coerceDeserializer(
			final SerializableEntity originalEntity,
			final AccountLookup accountLookup) throws IOException {
		// Arrange:
		final DeserializationContext context = new DeserializationContext(accountLookup);
		final HttpDeserializerResponseStrategy strategy = new HttpDeserializerResponseStrategy(context);
		final Response response = Mockito.mock(Response.class);
		Mockito.when(response.getStatus()).thenReturn(200);

		final byte[] serializedBytes = JsonSerializer.serializeToJson(originalEntity).toJSONString().getBytes();
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedBytes);
		final List<InputStreamResponseListener> listeners = new ArrayList<>();
		listeners.add(Mockito.mock(InputStreamResponseListener.class));
		Mockito.when(listeners.get(0).getInputStream()).thenReturn(inputStream);
		Mockito.when(response.getListeners(InputStreamResponseListener.class)).thenReturn(listeners);

		// Act:
		return strategy.coerce(Mockito.mock(Request.class), response);
	}
}
