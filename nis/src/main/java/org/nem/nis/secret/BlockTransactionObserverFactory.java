package org.nem.nis.secret;

import org.nem.core.model.observers.BalanceCommitTransferObserver;
import org.nem.nis.*;
import org.nem.nis.cache.*;

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
		return createBuilder(nisCache).build();
	}

	/**
	 * Creates a block transaction observer that commits all changes in reverse order.
	 * This observer is appropriate for an undo operation.
	 *
	 * @param nisCache The NIS cache.
	 * @return The observer.
	 */
	public BlockTransactionObserver createUndoCommitObserver(final NisCache nisCache) {
		return createBuilder(nisCache).buildReverse();
	}

	private static AggregateBlockTransactionObserverBuilder createBuilder(final NisCache nisCache) {
		final AccountStateRepository accountStateRepository = nisCache.getPoiFacade();
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();
		builder.add(new WeightedBalancesObserver(accountStateRepository));
		builder.add(new AccountsHeightObserver(nisCache));
		builder.add(new BalanceCommitTransferObserver(accountStateRepository));
		builder.add(new HarvestRewardCommitObserver(accountStateRepository));
		builder.add(new RemoteObserver(accountStateRepository));
		builder.add(new OutlinkObserver(accountStateRepository));
		builder.add(new PruningObserver(accountStateRepository, nisCache.getTransactionHashCache()));
		builder.add(new TransactionHashesObserver(nisCache.getTransactionHashCache()));
		return builder;
	}
}
