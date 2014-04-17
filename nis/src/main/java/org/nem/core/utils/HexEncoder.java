package org.nem.core.utils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * Static class that contains utility functions for converting hex strings to and from bytes.
 */
public class HexEncoder {

	/**
	 * Converts a hex string to a byte array.
	 *
	 * @param hexString The input hex string.
	 *
	 * @return The output byte array.
	 */
	public static byte[] getBytes(final String hexString) {
		final Hex codec = new Hex();
		final byte[] encodedBytes = StringEncoder.getBytes(hexString);

		try {
			return codec.decode(encodedBytes);
		} catch (DecoderException e) {
			throw new EncodingException(e);
		}
	}

	/**
	 * Converts a byte array to a hex string.
	 *
	 * @param bytes The input byte array.
	 *
	 * @return The output hex string.
	 */
	public static String getString(final byte[] bytes) {
		final Hex codec = new Hex();
		final byte[] decodedBytes = codec.encode(bytes);
		return StringEncoder.getString(decodedBytes);
	}
}
