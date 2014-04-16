package org.nem.nis.test;

import net.minidev.json.*;
import org.eclipse.jetty.client.api.*;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.nem.core.serialization.*;
import org.nem.core.test.MockAccountLookup;
import org.nem.nis.controller.utils.ErrorResponse;
import org.nem.peer.net.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * A helper class that connects to an NIS instance running on the local machine.
 */
public class LocalHostConnector {

	/**
	 * The result of a local host connection.
	 */
	public static class Result {

		private final int status;
		private final Object body;

		private Result(int status, final Object body) {
			this.status = status;
			this.body = body;
		}

		/**
		 * Gets the HTTP status.
		 *
		 * @return The http status.
		 */
		public int getStatus() { return this.status; }

		/**
		 * Gets the response body.
		 *
		 * @return The response body.
		 */
		public Object getBody() { return this.body; }

		/**
		 * Gets the response body as a String.
		 *
		 * @return The response body as a String.
		 */
		public String getBodyAsString() { return (String)this.body; }

		/**
		 * Gets the response body as an ErrorResponse.
		 *
		 * @return The response body as an ErrorResponse.
		 */
		public ErrorResponse getBodyAsErrorResponse() { return (ErrorResponse)this.body; }

		/**
		 * Gets the response body as a Deserializer.
		 *
		 * @return The response body as a Deserializer.
		 */
		public Deserializer getBodyAsDeserializer() {
			final DeserializationContext context = new DeserializationContext(new MockAccountLookup());
			return new JsonDeserializer((JSONObject)this.body, context);
		}
	}

	private final HttpMethodClient<Result> httpMethodClient = new HttpMethodClient<>(30);

	/**
	 * Returns the result of a POST operation to the specified path on the local host.
	 *
	 * @param path The path on the local host.
	 * @param input The POST input.
	 * @return The result of the POST operation.
	 */
	public Result post(final String path, final JSONObject input) {
		try {
			final URL url = new URL("http", "127.0.0.1", 7890, "/" + path);
			return this.httpMethodClient.post(url, input, new HttpResultResponseStrategy());
		} catch (MalformedURLException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	private static class HttpResultResponseStrategy implements HttpResponseStrategy<Result> {

		@Override
		public Result coerce(final Request request, final Response response) throws IOException {
			final List<InputStreamResponseListener> listeners = response.getListeners(InputStreamResponseListener.class);
			if (1 != listeners.size())
				throw new UnsupportedOperationException(String.format("Unexpected number of listeners: %d", listeners.size()));

			final InputStreamResponseListener listener = listeners.get(0);
			try (final InputStream responseStream = listener.getInputStream()) {
				return new Result(response.getStatus(), JSONValue.parse(responseStream));
			}
		}
	}
}
