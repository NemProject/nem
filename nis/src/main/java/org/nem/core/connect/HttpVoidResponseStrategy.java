package org.nem.core.connect;

import org.nem.core.utils.StringUtils;
import org.nem.core.serialization.*;

/**
 * Strategy for coercing an HTTP response into a null Deserializer.
 */
public class HttpVoidResponseStrategy extends HttpJsonResponseStrategy<Deserializer> {

	/**
	 * Coerces the parsed response stream into a deserializer.
	 *
	 * @param parsedStream The parsed response stream.
	 */
	protected Deserializer coerce(final Object parsedStream) {

		if (parsedStream instanceof String && StringUtils.isNullOrEmpty((String) parsedStream))
			return null;

		throw new FatalPeerException("Peer returned unexpected data");
	}
}