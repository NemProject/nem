package org.nem.core.connect;

import net.minidev.json.*;
import org.eclipse.jetty.client.api.*;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.springframework.http.*;
import org.nem.core.serialization.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Strategy for coercing an HTTP response into a Deserializer.
 */
public class HttpDeserializerResponseStrategy implements HttpResponseStrategy<Deserializer> {

	private final DeserializationContext context;

	/**
	 * Creates a new HTTP Deserializer response strategy.
	 *
	 * @param context The deserialization context to use when deserializing responses.
	 */
	public HttpDeserializerResponseStrategy(final DeserializationContext context) {
		this.context = context;
	}

	@Override
 	public Deserializer coerce(final Request request, final Response response) throws IOException {

		if (response.getStatus() != HttpStatus.OK.value())
			throw new InactivePeerException(String.format("Peer returned: %d", response.getStatus()));

		final List<InputStreamResponseListener> listeners = response.getListeners(InputStreamResponseListener.class);
		if (1 != listeners.size())
			throw new FatalPeerException(String.format("Unexpected number of listeners: %d", listeners.size()));

		final InputStreamResponseListener listener = listeners.get(0);
		try (final InputStream responseStream = listener.getInputStream()) {
			final Object parsedStream = JSONValue.parse(responseStream);
			if (!(parsedStream instanceof JSONObject))
				throw new FatalPeerException("Peer returned unexpected data");

			return new JsonDeserializer((JSONObject)parsedStream, this.context);
	}
}}