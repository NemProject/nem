package org.nem.core.connect;

import org.nem.core.serialization.*;

/**
 * Strategy for coercing an HTTP binary response into a deserializer.
 */
public class HttpBinaryResponseStrategy extends HttpDeserializerResponseStrategy {

	private final DeserializationContext context;

	/**
	 * Creates a new HTTP Deserializer response strategy.
	 *
	 * @param context The deserialization context to use when deserializing responses.
	 */
	public HttpBinaryResponseStrategy(final DeserializationContext context) {
		this.context = context;
	}

	@Override
	protected Deserializer coerce(final byte[] responseBytes) {
		try {
			return new BinaryDeserializer(responseBytes, this.context);
		} catch (final SerializationException e) {
			throw new FatalPeerException(e);
		}
	}

	@Override
	public String getSupportedContentType() {
		return ContentType.BINARY;
	}
}