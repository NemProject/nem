package org.nem.core.test;

import org.apache.http.*;
import org.apache.http.client.methods.HttpRequestBase;
import org.mockito.Mockito;
import org.nem.core.connect.*;
import org.nem.core.serialization.Deserializer;

import java.io.*;

/**
 * Static class containing test utilities shared across the connect package.
 */
public class ConnectUtils {

	/**
	 * Creates a deserializer using the specified strategy around the specified bytes.
	 *
	 * @param serializedBytes The bytes.
	 * @param strategy The strategy.
	 * @return The deserializer.
	 * @throws IOException On an IO error.
	 */
	public static Deserializer coerceDeserializer(
			final byte[] serializedBytes,
			final HttpDeserializerResponseStrategy strategy) throws IOException {
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

	/**
	 * Mocks the response to return the specified status code.
	 *
	 * @param response The response.
	 * @param statusCode The status code.
	 */
	public static void mockStatusCode(final HttpResponse response, final int statusCode) {
		// Arrange:
		final StatusLine statusLine = Mockito.mock(StatusLine.class);
		Mockito.when(response.getStatusLine()).thenReturn(statusLine);
		Mockito.when(statusLine.getStatusCode()).thenReturn(statusCode);
	}

	/**
	 * Attempts to coerce a stream that throws an IOException.
	 *
	 * @param strategy The strategy.
	 * @throws IOException On an IO error.
	 */
	public static void coerceStreamWithIoError(final HttpResponseStrategy<?> strategy) throws IOException {
		// Arrange:
		final HttpResponse response = Mockito.mock(HttpResponse.class);
		mockStatusCode(response, 200);

		final HttpEntity entity = Mockito.mock(HttpEntity.class);
		Mockito.when(response.getEntity()).thenReturn(entity);
		Mockito.when(entity.getContent()).thenThrow(new IOException());

		// Act:
		strategy.coerce(Mockito.mock(HttpRequestBase.class), response);
	}
}
