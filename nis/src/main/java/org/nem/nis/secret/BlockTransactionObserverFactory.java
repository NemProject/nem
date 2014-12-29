package org.nem.nis.secret;

import org.nem.core.model.observers.BalanceCommitTransferObserver;
import org.nem.nis.cache.*;

import java.util.EnumSet;

/**
 * Factory for creating BlockTransactionObserver objects.
 */
public class BlockTransactionObserverFactory {

	/**
	 * Creates a block transaction observer that commits all changes in order.
	 * This observer is appropriate for an execute operation.
	 *
	 * @param nisCache The NIS cache.
	 * @return The observer.
	 */
	public BlockTransactionObserver createExecuteCommitObserver(final NisCache nisCache) {
		return this.createExecuteCommitObserver(nisCache, EnumSet.of(ObserverOption.Default));
	}

	/**
	 * Creates a block transaction observer that commits all changes in order with the specified options.
	 * This observer is appropriate for an execute operation.
	 *
	 * @param nisCache The NIS cache.
	 * @param options The options.
	 * @return The observer.
	 */
	public BlockTransactionObserver createExecuteCommitObserver(final NisCache nisCache, final EnumSet<ObserverOption> options) {
		return this.createBuilder(nisCache, options).build();
	}

	/**
	 * Creates a block transaction observer that commits all changes in reverse order.
	 * This observer is appropriate for an undo operation.
	 *
	 * @param nisCache The NIS cache.
	 * @return The observer.
	 */
	public BlockTransactionObserver createUndoCommitObserver(final NisCache nisCache) {
		return this.createUndoCommitObserver(nisCache, EnumSet.of(ObserverOption.Default));
	}

	/**
	 * Creates a block transaction observer that commits all changes in reverse order.
	 * This observer is appropriate for an undo operation.
	 *
	 * @param nisCache The NIS cache.
	 * @param options The options.
	 * @return The observer.
	 */
	public BlockTransactionObserver createUndoCommitObserver(final NisCache nisCache, final EnumSet<ObserverOption> options) {
		return this.createBuilder(nisCache, options).buildReverse();
	}

	private AggregateBlockTransactionObserverBuilder createBuilder(final NisCache nisCache, final EnumSet<ObserverOption> options) {
		final AccountStateCache accountStateCache = nisCache.getAccountStateCache();
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();
		builder.add(new WeightedBalancesObserver(accountStateCache));
		builder.add(new AccountsHeightObserver(nisCache));
		builder.add(new BalanceCommitTransferObserver(accountStateCache));
		builder.add(new HarvestRewardCommitObserver(accountStateCache));
		builder.add(new RemoteObserver(accountStateCache));
		builder.add(new MultisigAccountObserver(accountStateCache));
		builder.add(new OutlinkObserver(accountStateCache));
		builder.add(new PruningObserver(accountStateCache, nisCache.getTransactionHashCache()));
		builder.add(new TransactionHashesObserver(nisCache.getTransactionHashCache()));

		if (!options.contains(ObserverOption.NoIncrementalPoi)) {
			builder.add(new RecalculateImportancesObserver(nisCache));
		}

		return builder;
	}
}
