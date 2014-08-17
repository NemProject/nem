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

	/**
	 * Replaces a variable contained in a string with a value. A variable is defined as ${variable}.
	 * This pattern is replaced by the given value.
	 * TODO-CR: 20140817 J->T please be consistent in documentation; each parameter documentation should
	 * begin with a capital letter and end with a '.' (i.e. be a sentence), and no need for '-'.
	 *
	 * @param string - string that contains variables
	 * @param name - name of the variable to be replaced with its value
	 * @param value - value that will replace the variable.
	 * @return string with value replacing the variable with the given name
	 */
	public static String replaceVariable(final String string, final String name, final String value) {
		// TODO-CR: 20140817 J->T why are you changing $ to §?
		return string.replace("$", "§").replace(String.format("§{%s}", name), value); // Did not get the regex $ escaped, so this work-around
	}
}
