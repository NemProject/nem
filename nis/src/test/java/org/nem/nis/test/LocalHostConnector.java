package org.nem.nis.test;

import net.minidev.json.JSONObject;
import org.nem.core.connect.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.utils.ExceptionUtils;

import java.net.URL;

/**
 * A helper class that connects to an NIS instance running on the local machine.
 */
public class LocalHostConnector {

	private final HttpMethodClient<ErrorResponseDeserializerUnion> httpMethodClient = new HttpMethodClient<>();

	/**
	 * Returns the result of a POST operation to the specified path on the local host.
	 *
	 * @param path The path on the local host.
	 * @param input The POST input.
	 * @return The result of the POST operation.
	 */
	public ErrorResponseDeserializerUnion post(final String path, final JSONObject input) {
		final DeserializationContext context = new DeserializationContext(new MockAccountLookup());
		final HttpErrorResponseDeserializerUnionStrategy strategy = new HttpErrorResponseDeserializerUnionStrategy(context);
		return ExceptionUtils.propagate(() -> {
			final URL url = new URL("http", "127.0.0.1", 7890, "/" + path);
			return this.httpMethodClient.post(url, new HttpJsonPostRequest(input), strategy).get();
		});
	}
}
