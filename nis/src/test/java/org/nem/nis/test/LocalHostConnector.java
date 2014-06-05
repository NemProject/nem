package org.nem.nis.test;

import net.minidev.json.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.nem.core.connect.*;
import org.nem.core.serialization.*;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.controller.utils.ErrorResponse;

import java.net.URL;

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
		public ErrorResponse getBodyAsErrorResponse() { return new ErrorResponse(this.getBodyAsDeserializer()); }

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

	private final HttpMethodClient<Result> httpMethodClient = new HttpMethodClient<>(10000);

	/**
	 * Returns the result of a POST operation to the specified path on the local host.
	 *
	 * @param path The path on the local host.
	 * @param input The POST input.
	 * @return The result of the POST operation.
	 */
	public Result post(final String path, final JSONObject input) {
		return ExceptionUtils.propagate(() -> {
			final URL url = new URL("http", "127.0.0.1", 7890, "/" + path);
			return this.httpMethodClient.post(url, input, new HttpResultResponseStrategy()).get();
		});
	}

	private static class HttpResultResponseStrategy implements HttpResponseStrategy<Result> {

		@Override
		public Result coerce(final HttpRequestBase request, final HttpResponse response) {
			return ExceptionUtils.propagate(() ->
				new Result(
						response.getStatusLine().getStatusCode(),
						JSONValue.parse(response.getEntity().getContent())));
		}
	}
}
