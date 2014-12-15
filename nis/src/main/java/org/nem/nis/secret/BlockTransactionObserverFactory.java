package org.nem.nis.secret;

import org.nem.core.model.observers.BalanceCommitTransferObserver;
import org.nem.nis.cache.*;

/**
 * Factory for creating BlockTransactionObserver objects.
 */
public class BlockTransactionObserverFactory {

	// TODO 20141214 should probably improve the tests for this class to match the other factories.
	// TODO 20141214 should use a real enum

	/**
	 * Options to customize the observers created by this factory.
	 */
	public static class Options {
		/**
		 * The default options.
		 */
		public final static int Default = 0;

		/**
		 * Excludes the incremental poi observer.
		 */
		public final static int NoIncrementalPoi = 1;
	}

	/**
	 * Creates a block transaction observer that commits all changes in order.
	 * This observer is appropriate for an execute operation.
	 *
	 * @param nisCache The NIS cache.
	 * @return The observer.
	 */
	public BlockTransactionObserver createExecuteCommitObserver(final NisCache nisCache) {
		return this.createExecuteCommitObserver(nisCache, Options.Default);
	}

	/**
	 * Creates a block transaction observer that commits all changes in order with the specified options.
	 * This observer is appropriate for an execute operation.
	 *
	 * @param nisCache The NIS cache.
	 * @param options The options.
	 * @return The observer.
	 */
	public BlockTransactionObserver createExecuteCommitObserver(final NisCache nisCache, final int options) {
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
		return this.createUndoCommitObserver(nisCache, Options.Default);
	}

	/**
	 * Creates a block transaction observer that commits all changes in reverse order.
	 * This observer is appropriate for an undo operation.
	 *
	 * @param nisCache The NIS cache.
	 * @param options The options.
	 * @return The observer.
	 */
	public BlockTransactionObserver createUndoCommitObserver(final NisCache nisCache, final int options) {
		return this.createBuilder(nisCache, options).buildReverse();
	}

	private AggregateBlockTransactionObserverBuilder createBuilder(final NisCache nisCache, final int options) {
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

		if (0 == (options & Options.NoIncrementalPoi)) {
			builder.add(new RecalculateImportancesObserver(nisCache));
		}

		return builder;
	}
}
