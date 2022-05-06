package org.nem.nis.harvesting;

import org.nem.core.model.primitive.*;
import org.nem.nis.state.ReadOnlyAccountState;

import java.util.function.Function;

/**
 * Predicate class that can be used to check if an account is eligible for harvesting.
 */
public class CanHarvestPredicate {
	private final Function<BlockHeight, Amount> getMinHarvesterBalance;

	/**
	 * Creates a new predicate.
	 *
	 * @param minHarvesterBalance The (constant) minimum balance required for a harvester.
	 */
	public CanHarvestPredicate(final Amount minHarvesterBalance) {
		this.getMinHarvesterBalance = height -> minHarvesterBalance;
	}

	/**
	 * Creates a new predicate.
	 *
	 * @param getMinHarvesterBalance A function that returns the min harvester balance given a block height.
	 */
	public CanHarvestPredicate(final Function<BlockHeight, Amount> getMinHarvesterBalance) {
		this.getMinHarvesterBalance = getMinHarvesterBalance;
	}

	/**
	 * Determines whether or not the account is eligible for harvesting at the specified block height.
	 *
	 * @param accountState The account's state.
	 * @param height The height.
	 * @return true if the account is eligible.
	 */
	public boolean canHarvest(final ReadOnlyAccountState accountState, final BlockHeight height) {
		final Amount minHarvesterBalance = this.getMinHarvesterBalance.apply(height);
		return accountState.getWeightedBalances().getVested(height).compareTo(minHarvesterBalance) >= 0;
	}
}
