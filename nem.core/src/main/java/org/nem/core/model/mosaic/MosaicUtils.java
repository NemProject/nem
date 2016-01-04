package org.nem.core.model.mosaic;

import org.nem.core.model.primitive.*;

/**
 * Static class containing mosaic helper functions.
 */
public class MosaicUtils {

	/**
	 * Tries to add supplies representing assets corresponding to a mosaic with the specified divisibility.
	 *
	 * @param divisibility The mosaic divisibility.
	 * @param s1 The first supply.
	 * @param s2 The second supply.
	 * @return The result or null if the sum will violate a constraint.
	 */
	public static Supply tryAdd(final int divisibility, final Supply s1, final Supply s2) {
		final long maxQuantity = MosaicConstants.MAX_QUANTITY / getMultipler(divisibility);
		final Supply sum = s1.add(s2);
		return sum.getRaw() > maxQuantity ? null : sum;
	}

	/**
	 * Adds supplies representing assets corresponding to a mosaic with the specified divisibility.
	 * Throws an exception if the supplies cannot be added.
	 *
	 * @param divisibility The mosaic divisibility.
	 * @param s1 The first supply.
	 * @param s2 The second supply.
	 * @return The result.
	 */
	public static Supply add(final int divisibility, final Supply s1, final Supply s2) {
		final Supply sum = MosaicUtils.tryAdd(divisibility, s1, s2);
		if (null == sum) {
			final String message = String.format("cannot add %s to %s for mosaic with divisibility %d", s1, s2, divisibility);
			throw new IllegalArgumentException(message);
		}

		return sum;
	}

	/**
	 * Converts a supply to a quantity.
	 *
	 * @param supply The supply.
	 * @param divisibility The mosaic divisibility.
	 * @return The quantity.
	 */
	public static Quantity toQuantity(final Supply supply, final int divisibility) {
		return new Quantity(supply.getRaw() * getMultipler(divisibility));
	}

	/**
	 * Converts a quantity to a (truncated) supply.
	 *
	 * @param quantity The quantity.
	 * @param divisibility The mosaic divisibility.
	 * @return The supply.
	 */
	public static Supply toSupply(final Quantity quantity, final int divisibility) {
		return new Supply(quantity.getRaw() / getMultipler(divisibility));
	}

	private static int getMultipler(int divisibility) {
		int multiplier = 1;
		while (divisibility > 0) {
			--divisibility;
			multiplier *= 10;
		}

		return multiplier;
	}
}
