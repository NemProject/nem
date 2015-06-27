package org.nem.nis.secret;

import org.nem.nis.cache.*;
import org.nem.nis.secret.pruning.*;

import java.util.EnumSet;

/**
 * Factory for creating BlockTransactionObserver objects.
 */
public class BlockTransactionObserverFactory {
	private final EnumSet<ObserverOption> observerOptions;

	/**
	 * Creates a new transaction observer factory with no additional options.
	 */
	public BlockTransactionObserverFactory() {
		this.observerOptions = EnumSet.noneOf(ObserverOption.class);
	}

	/**
	 * Creates a new transaction observer factory which uses additional options.
	 */
	public BlockTransactionObserverFactory(final EnumSet<ObserverOption> observerOptions) {
		this.observerOptions = observerOptions;
	}

	/**
	 * Creates a block transaction observer that commits all changes in order.
	 * This observer is appropriate for an execute operation.
	 *
	 * @param nisCache The NIS cache.
	 * @return The observer.
	 */
	public BlockTransactionObserver createExecuteCommitObserver(final NisCache nisCache) {
		return this.createBuilder(nisCache, this.observerOptions).build();
	}

	/**
	 * Creates a block transaction observer that commits all changes in reverse order.
	 * This observer is appropriate for an undo operation.
	 *
	 * @param nisCache The NIS cache.
	 * @return The observer.
	 */
	public BlockTransactionObserver createUndoCommitObserver(final NisCache nisCache) {
		return this.createBuilder(nisCache, this.observerOptions).buildReverse();
	}

	private AggregateBlockTransactionObserverBuilder createBuilder(final NisCache nisCache, final EnumSet<ObserverOption> options) {
		final AccountStateCache accountStateCache = nisCache.getAccountStateCache();
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();
		builder.add(new WeightedBalancesObserver(accountStateCache));
		builder.add(new AccountsHeightObserver(nisCache));
		builder.add(new BalanceCommitTransferObserver(accountStateCache));
		builder.add(new HarvestRewardCommitObserver(accountStateCache));
		builder.add(new RemoteObserver(accountStateCache));
		builder.add(new MultisigCosignatoryModificationObserver(accountStateCache));
		builder.add(new MultisigMinCosignatoriesModificationObserver(accountStateCache));
		builder.add(new OutlinkObserver(accountStateCache));
		builder.add(new TransactionHashesObserver(nisCache.getTransactionHashCache()));
		builder.add(new ProvisionNamespaceObserver(nisCache.getNamespaceCache()));

		// pruners
		builder.add(new AccountStateCachePruningObserver(nisCache.getAccountStateCache(), !options.contains(ObserverOption.NoHistoricalDataPruning)));
		builder.add(new NamespaceCachePruningObserver(nisCache.getNamespaceCache()));
		builder.add(new TransactionHashCachePruningObserver(nisCache.getTransactionHashCache()));

		if (!options.contains(ObserverOption.NoIncrementalPoi)) {
			builder.add(new RecalculateImportancesObserver(nisCache));
		}

		return builder;
	}
}
