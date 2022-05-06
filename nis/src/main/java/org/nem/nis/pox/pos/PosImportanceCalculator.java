package org.nem.nis.pox.pos;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.pox.ImportanceCalculator;
import org.nem.nis.state.*;

import java.util.Collection;

/**
 * Class that implements POS (proof of stake).
 */
public class PosImportanceCalculator implements ImportanceCalculator {

	@Override
	public void recalculate(final BlockHeight blockHeight, final Collection<AccountState> accountStates) {
		final double cumulativeBalance = (double) this.getCumulativeBalance(blockHeight, accountStates);
		accountStates.stream().forEach(state -> {
			final AccountImportance accountImportance = state.getImportanceInfo();
			final double importance = state.getWeightedBalances().getVested(blockHeight).getNumMicroNem() / cumulativeBalance;
			accountImportance.setLastPageRank(0.0);
			accountImportance.setImportance(blockHeight, importance);

			// on machines that do not support historical information, each HistoricalImportances will only contain a
			// single entry in-between pruning
			final HistoricalImportances historicalImportances = state.getHistoricalImportances();
			historicalImportances.addHistoricalImportance(new AccountImportance(blockHeight, importance, 0.0));
		});
	}

	private long getCumulativeBalance(final BlockHeight blockHeight, final Collection<AccountState> accountStates) {
		return accountStates.stream().map(state -> state.getWeightedBalances().getVested(blockHeight).getNumMicroNem()).reduce(0L,
				Long::sum);
	}
}
