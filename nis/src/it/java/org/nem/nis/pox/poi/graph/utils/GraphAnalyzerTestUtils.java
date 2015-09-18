package org.nem.nis.pox.poi.graph.utils;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.nis.state.AccountState;

/**
 * Test utils used by the graph integration / analysis tests.
 */
public class GraphAnalyzerTestUtils {
	private static final BalanceComparisonType BALANCE_COMPARISON_TYPE = BalanceComparisonType.VestedOnly;

	private enum BalanceComparisonType {
		VestedOnly,
		VestedAndUnvested
	}

	/**
	 * Maps a poi account state to a balance.
	 *
	 * @param accountState The account state.
	 * @param blockHeight The block height.
	 * @return The balance.
	 */
	public static Amount mapPoiAccountStateToBalance(final AccountState accountState, final BlockHeight blockHeight) {
		final Amount vested = accountState.getWeightedBalances().getVested(blockHeight);
		final Amount unvested = accountState.getWeightedBalances().getUnvested(blockHeight);
		final Amount total = vested.add(unvested);
		return BALANCE_COMPARISON_TYPE == BalanceComparisonType.VestedAndUnvested ? total : vested;
	}

	/**
	 * Creates an account with the specified balance.
	 *
	 * @param address The account address.
	 * @param blockHeight The block height.
	 * @param amount The balance.
	 * @return The account state.
	 */
	public static AccountState createAccountWithBalance(final Address address, final long blockHeight, final Amount amount) {
		final AccountState state = new AccountState(address);
		state.getWeightedBalances().addFullyVested(new BlockHeight(blockHeight), amount);
		return state;
	}
}
