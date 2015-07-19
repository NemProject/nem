package org.nem.core.model.mosaic;

import org.nem.core.model.primitive.Quantity;

/**
 * Static class containing mosaic helper functions.
 */
public class MosaicUtils {

	/**
	 * Tries to add quantities representing assets corresponding to a mosaic with the specified divisibility.
	 *
	 * @param divisibility The mosaic divisibility.
	 * @param q1 The first quantity.
	 * @param q2 The second quantity.
	 * @return The result or null if the sum will violate a constraint.
	 */
	public static Quantity tryAdd(int divisibility, final Quantity q1, final Quantity q2) {
		long maxQuantity = MosaicConstants.MAX_QUANTITY;

		//noinspection StatementWithEmptyBody
		for (; divisibility > 0; --divisibility, maxQuantity /= 10);

		final Quantity sum = q1.add(q2);
		return sum.getRaw() > maxQuantity ? null : sum;
	}

	/**
	 * Adds quantities representing assets corresponding to a mosaic with the specified divisibility.
	 * Throws an exception if the quantities cannot be added.
	 *
	 * @param divisibility The mosaic divisibility.
	 * @param q1 The first quantity.
	 * @param q2 The second quantity.
	 * @return The result.
	 */
	public static Quantity add(final int divisibility, final Quantity q1, final Quantity q2) {
		final Quantity sum = MosaicUtils.tryAdd(divisibility, q1, q2);
		if (null == sum) {
			final String message = String.format("cannot add %s to %s for mosaic with divisibility %d", q1, q2, divisibility);
			throw new IllegalArgumentException(message);
		}

		return sum;
	}
}
