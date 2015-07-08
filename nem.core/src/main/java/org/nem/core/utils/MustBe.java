package org.nem.core.utils;

import org.nem.core.model.primitive.Quantity;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Helper class for validating parameters.
 */
public class MustBe {

	/**
	 * Throws an exception if the specified object is null.
	 *
	 * @param obj The object.
	 * @param name The object name.
	 */
	public static void notNull(final Object obj, final String name) {
		if (null == obj) {
			final String message = String.format("%s cannot be null", name);
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Throws an exception if the specified string does not match the pattern, is empty, or longer than the max length.
	 *
	 * @param str The string.
	 * @param name The string name.
	 * @param pattern The pattern to match.
	 * @param maxLength The max length.
	 */
	public static void match(final String str, final String name, final Pattern pattern, final int maxLength) {
		if (null == str || str.isEmpty() || str.length() > maxLength || !pattern.matcher(str).matches()) {
			final String message = String.format("%s does not match the desired pattern", name);
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Throws an exception if the specified integer value is not in the specified inclusive range.
	 *
	 * @param value The integer value.
	 * @param name The value name.
	 * @param minInclusive The min allowed value (inclusive).
	 * @param maxInclusive The max allowed value (inclusive).
	 */
	public static void inRange(final int value, final String name, final int minInclusive, final int maxInclusive) {
		inRange((long)value, name, minInclusive, maxInclusive);
	}

	/**
	 * Throws an exception if the specified long value is not in the specified inclusive range.
	 *
	 * @param value The long value.
	 * @param name The value name.
	 * @param minInclusive The min allowed value (inclusive).
	 * @param maxInclusive The max allowed value (inclusive).
	 */
	public static void inRange(final long value, final String name, final long minInclusive, final long maxInclusive) {
		if (value < minInclusive || value > maxInclusive) {
			final String message = String.format("%s must be between %d and %d inclusive", name, minInclusive, maxInclusive);
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Throws an exception if the specified amount is not positive.
	 *
	 * @param amount The amount.
	 * @param name The amount name.
	 */
	public static void positive(final Quantity amount, final String name) {
		if (Quantity.ZERO.equals(amount)) {
			final String message = String.format("%s must be positive", name);
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Throws an exception if the specified collection is not empty.
	 *
	 * @param collection The collection.
	 * @param name The collection name.
	 */
	public static void empty(final Collection<?> collection, final String name) {
		if (!collection.isEmpty()) {
			final String message = String.format("%s must be empty", name);
			throw new IllegalArgumentException(message);
		}
	}
}
