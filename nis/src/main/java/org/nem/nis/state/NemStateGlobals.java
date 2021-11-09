package org.nem.nis.state;

import org.nem.core.utils.SetOnce;

import java.util.function.Supplier;

/**
 * Helper class for storing NEM globals related to behavior of nem state. <br>
 * This class should really be used sparingly!
 */
public class NemStateGlobals {
	private static final SetOnce<Supplier<WeightedBalances>> WEIGHTED_BALANCES_SUPPLIER = new SetOnce<>(
			TimeBasedVestingWeightedBalances::new);

	/**
	 * Creates a weighted balances object using the currently configured policy.
	 *
	 * @return The weighted balances.
	 */
	public static WeightedBalances createWeightedBalances() {
		return WEIGHTED_BALANCES_SUPPLIER.get().get();
	}

	/**
	 * Sets the global weighted balances supplier.
	 *
	 * @param supplier The weighted balances supplier.
	 */
	public static void setWeightedBalancesSupplier(final Supplier<WeightedBalances> supplier) {
		WEIGHTED_BALANCES_SUPPLIER.set(supplier);
	}
}
