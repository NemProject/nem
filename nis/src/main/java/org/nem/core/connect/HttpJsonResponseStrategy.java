package org.nem.core.connect;

import net.minidev.json.*;
import org.eclipse.jetty.client.api.*;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.springframework.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Strategy for coercing an HTTP JSON response.
 *
 * @param <T> The result type.
 */
public abstract class HttpJsonResponseStrategy<T> implements HttpResponseStrategy<T> {

	@Override
	public final T coerce(final Request request, final Response response) throws IOException {
		if (response.getStatus() != HttpStatus.OK.value())
			throw new InactivePeerException(String.format("Peer returned: %d", response.getStatus()));

		final List<InputStreamResponseListener> listeners = response.getListeners(InputStreamResponseListener.class);
		if (1 != listeners.size())
			throw new FatalPeerException(String.format("Unexpected number of listeners: %d", listeners.size()));

		final InputStreamResponseListener listener = listeners.get(0);
		try (final InputStream responseStream = listener.getInputStream()) {
			return this.coerce(JSONValue.parse(responseStream));
		}
	}

	/**
	 * Coerces the parsed response stream into a deserializer.
	 *
	 * @param parsedStream The parsed response stream.
	 */
	protected abstract T coerce(final Object parsedStream);
}