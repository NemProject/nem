package org.nem.core.connect;

import net.minidev.json.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.http.*;

import java.io.*;

/**
 * Strategy for coercing an HTTP JSON response.
 *
 * @param <T> The result type.
 */
public abstract class HttpJsonResponseStrategy<T> implements HttpResponseStrategy<T> {

	@Override
	public T coerce(final HttpRequestBase request, final HttpResponse response) {
		try {
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.OK.value())
				throw new FatalPeerException(String.format("Peer returned: %d", statusCode));

			try (final InputStream responseStream = response.getEntity().getContent()) {
				return this.coerce(JSONValue.parse(responseStream));
			}
		} catch (final IOException e) {
			throw new FatalPeerException(e);
		}
	}

	/**
	 * Coerces the parsed response stream into a deserializer.
	 *
	 * @param parsedStream The parsed response stream.
	 */
	protected abstract T coerce(final Object parsedStream);
}