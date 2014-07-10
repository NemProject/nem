package org.nem.core.connect;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.nem.core.serialization.*;
import org.springframework.http.HttpStatus;

import java.io.*;

/**
 * Strategy for coercing an HTTP response into a Deserializer.
 */
public abstract class HttpDeserializerResponseStrategy implements HttpResponseStrategy<Deserializer> {

	@Override
	public Deserializer coerce(final HttpRequestBase request, final HttpResponse response) {
		try {
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.OK.value())
				throw new FatalPeerException(String.format("Peer returned: %d", statusCode));

			try (final InputStream responseStream = response.getEntity().getContent()) {
				final byte[] responseBytes = sun.misc.IOUtils.readFully(responseStream, -1, true);
				return this.coerce(responseBytes);
			}
		} catch (final IOException e) {
			throw new FatalPeerException(e);
		}
	}

	/**
	 * Coerces the raw response bytes into a deserializer.
	 *
	 * @param responseBytes The raw response bytes.
	 */
	protected abstract Deserializer coerce(final byte[] responseBytes);
}