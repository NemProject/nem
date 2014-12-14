package org.nem.nis.harvesting;

import org.nem.core.model.primitive.*;
import org.nem.nis.state.ReadOnlyAccountState;

/**
 * Predicate class that can be used to check if an account is eligible for harvesting.
 */
public class CanHarvestPredicate {
	private final Amount minHarvesterBalance;

	/**
	 * Creates a new predicate.
	 *
	 * @param minHarvesterBalance The minimum balance required for a harvester.
	 */
	public CanHarvestPredicate(final Amount minHarvesterBalance) {
		this.minHarvesterBalance = minHarvesterBalance;
	}

	/**
	 * Determines whether or not the account is eligible for harvesting at the specified block height.
	 *
	 * @param accountState The account's state.
	 * @param height The height.
	 * @return true if the account is eligible.
	 */
	public boolean canHarvest(final ReadOnlyAccountState accountState, final BlockHeight height) {
		return accountState.getWeightedBalances().getVested(height).compareTo(this.minHarvesterBalance) >= 0;
	}
}