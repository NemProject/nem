package org.nem.core.utils;

import org.nem.core.model.primitive.*;

import java.util.Collection;

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
	 * Throws an exception if the specified amount is not positive.
	 *
	 * @param amount The amount.
	 * @param name The amount name.
	 */
	public static void positive(final GenericAmount amount, final String name) {
		if (GenericAmount.ZERO.equals(amount)) {
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
