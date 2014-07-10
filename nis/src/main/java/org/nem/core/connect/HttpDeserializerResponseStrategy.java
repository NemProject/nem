package org.nem.core.connect;

import net.minidev.json.*;
import org.nem.core.serialization.*;

/**
 * Strategy for coercing an HTTP response into a Deserializer.
 */
public class HttpDeserializerResponseStrategy extends HttpJsonResponseStrategy<Deserializer> {

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
	protected Deserializer coerce(final byte[] responseBytes) {
		final Object parsedStream = JSONValue.parse(responseBytes);
		if (parsedStream instanceof JSONObject)
			return new JsonDeserializer((JSONObject)parsedStream, this.context);

		throw new FatalPeerException("Peer returned unexpected data");
	}
}