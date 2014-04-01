package org.nem.core.utils;

import org.nem.core.serialization.SerializationException;

import java.io.UnsupportedEncodingException;

/**
 * Static class that contains utility functions for converting strings to and from UTF-8 bytes.
 */
public class StringEncoder {

	private static final String ENCODING_NAME = "UTF-8";

	/**
	 * Converts a string to a UTF-8 byte array.
	 *
	 * @param s The input string.
	 *
	 * @return The output byte array.
	 */
	public static byte[] getBytes(final String s) {
		try {
			return s.getBytes(ENCODING_NAME);
		} catch (UnsupportedEncodingException e) {
			throw new SerializationException(e);
		}
	}

	/**
	 * Converts a UTF-8 byte array to a string.
	 *
	 * @param bytes The input byte array.
	 *
	 * @return The output string.
	 */
	public static String getString(byte[] bytes) {
		try {
			return new String(bytes, ENCODING_NAME);
		} catch (UnsupportedEncodingException e) {
			throw new SerializationException(e);
		}
	}
}
