package org.nem.nis.secret.pruning;

import org.nem.core.model.NemGlobals;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.secret.BlockNotificationContext;
import org.nem.nis.state.AccountState;

/**
 * Pruning observer that prunes the account state cache.
 */
public class AccountStateCachePruningObserver extends AbstractPruningObserver {
	private final AccountStateCache accountStateCache;
	private final boolean pruneHistoricalData;

	/**
	 * Creates a new observer.
	 *
	 * @param accountStateCache The account state cache.
	 * @param pruneHistoricalData Flag indicating if historical data should be pruned.
	 */
	public AccountStateCachePruningObserver(final AccountStateCache accountStateCache, final boolean pruneHistoricalData) {
		this.accountStateCache = accountStateCache;
		this.pruneHistoricalData = pruneHistoricalData;
	}

	@Override
	protected void prune(final BlockNotificationContext context) {
		// keep 1 day of weighted balance history, 31 days of outlink history (keep an extra day so that calculations are correct after
		// rollbacks)
		final long blocksPerDay = NemGlobals.getBlockChainConfiguration().getEstimatedBlocksPerDay();
		final long outlinkBlockHistory = NemGlobals.getBlockChainConfiguration().getEstimatedBlocksPerMonth() + blocksPerDay;
		final BlockHeight weightedBalancePruneHeight = getPruneHeight(context.getHeight(), blocksPerDay);
		final BlockHeight outlinkPruneHeight = getPruneHeight(context.getHeight(), outlinkBlockHistory);
		for (final AccountState accountState : this.accountStateCache.mutableContents()) {
			if (this.pruneHistoricalData) {
				accountState.getWeightedBalances().prune(weightedBalancePruneHeight);
				accountState.getHistoricalImportances().prune();
			}

			accountState.getImportanceInfo().prune(outlinkPruneHeight);
		}
	}
}
