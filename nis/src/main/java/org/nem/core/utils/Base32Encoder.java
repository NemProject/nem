package org.nem.core.utils;

import org.apache.commons.codec.binary.Base32;

/**
 * Static class that contains utility functions for converting Base32 strings to and from bytes.
 */
public class Base32Encoder {

	/**
	 * Converts a string to a byte array.
	 *
	 * @param base32String The input Base32 string.
	 *
	 * @return The output byte array.
	 */
	public static byte[] getBytes(final String base32String) {
		Base32 codec = new Base32();
		byte[] encodedBytes = StringEncoder.getBytes(base32String);
		if (!codec.isInAlphabet(encodedBytes, true))
			throw new IllegalArgumentException("malformed base32 string passed to getBytes");

		return codec.decode(encodedBytes);
	}

	/**
	 * Converts a byte array to a Base32 string.
	 *
	 * @param bytes The input byte array.
	 *
	 * @return The output Base32 string.
	 */
	public static String getString(byte[] bytes) {
		Base32 codec = new Base32();
		byte[] decodedBytes = codec.encode(bytes);
		return StringEncoder.getString(decodedBytes);
	}
}
