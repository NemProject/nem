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
	public static Supply tryAdd(int divisibility, final Supply s1, final Supply s2) {
		long maxQuantity = MosaicConstants.MAX_QUANTITY;

		//noinspection StatementWithEmptyBody
		for (; divisibility > 0; --divisibility, maxQuantity /= 10);

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
}
