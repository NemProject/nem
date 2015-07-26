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

		// TODO 20150725 BR -> J: Consider the following example:
		// > You have a mosaic foo with divisibility of 6. You want to have an initial supply of 1_000_000 foo. That should be possible.
		// > Due to the divisibility the property "quantity" has to be set to 1_000_000_000_000.
		// > During validation of the mosaic properties tryAdd is called with divisibility = 6, q1 = 0 and q2 = 1_000_000_000_000.
		// > The loop below lowers maxQuantity to  9_000_000_000_000_000 / 1_000_000 = 9_000_000_000.
		// > Therefore q1 + q2 = 1_000_000_000_000 > 9_000_000_000 = maxQuantity and null will be returned.
		// > tryAdd only works if the number of units is used as input, not the number of fractional units.
		// > (look at test canAddUpToAdjustedMaxQuantityWhenDivisibilityIsNonZero)
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
