package org.nem.core.connect;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.nem.core.serialization.Deserializer;
import org.nem.core.utils.*;

import java.io.*;

/**
 * Strategy for coercing an HTTP response into a Deserializer.
 */
public abstract class HttpDeserializerResponseStrategy implements HttpResponseStrategy<Deserializer> {

	@Override
	public Deserializer coerce(final HttpRequestBase request, final HttpResponse response) {
		try {
			final int statusCode = response.getStatusLine().getStatusCode();

			try (final InputStream responseStream = response.getEntity().getContent()) {
				final byte[] responseBytes = IOUtils.toByteArray(responseStream);

				if (statusCode != HttpStatus.OK.value()) {
					final String message = String.format(
							"Peer returned %s with error: <%s>",
							statusCode,
							StringEncoder.getString(responseBytes));
					throw new FatalPeerException(message);
				}

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
	 * @return The deserializer.
	 */
	protected abstract Deserializer coerce(final byte[] responseBytes);
}