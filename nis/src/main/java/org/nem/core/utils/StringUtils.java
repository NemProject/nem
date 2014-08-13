package org.nem.core.utils;

/**
 * Static class that contains string utility functions.
 */
public class StringUtils {

	/**
	 * Determines if the specified string is null or empty.
	 *
	 * @param str The string.
	 * @return true if the string is null or empty.
	 */
	public static boolean isNullOrEmpty(final String str) {
		return null == str || str.isEmpty();
	}

	/**
	 * Determines if the specified string is null or whitespace.
	 *
	 * @param str The string.
	 * @return true if the string is null or whitespace.
	 */
	public static boolean isNullOrWhitespace(final String str) {
		if (isNullOrEmpty(str)) {
			return true;
		}

		for (int i = 0; i < str.length(); i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return false;
			}
		}

		return true;
	}
}
