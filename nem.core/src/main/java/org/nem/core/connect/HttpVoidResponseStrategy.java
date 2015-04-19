package org.nem.core.connect;

import org.nem.core.serialization.Deserializer;

/**
 * Strategy for coercing an HTTP response into a null Deserializer.
 */
public class HttpVoidResponseStrategy extends HttpDeserializerResponseStrategy {

	@Override
	protected Deserializer coerce(final byte[] responseBytes) {
		if (0 == responseBytes.length) {
			return null;
		}

		throw new FatalPeerException(String.format("Peer returned unexpected data (length %d)", responseBytes.length));
	}

	@Override
	public String getSupportedContentType() {
		return ContentType.VOID;
	}
}