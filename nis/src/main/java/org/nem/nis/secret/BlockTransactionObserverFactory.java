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
		final AccoutStateRepository accoutStateRepository = nisCache.getPoiFacade();
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();
		builder.add(new WeightedBalancesObserver(accoutStateRepository));
		builder.add(new AccountsHeightObserver(nisCache));
		builder.add(new BalanceCommitTransferObserver(accoutStateRepository));
		builder.add(new HarvestRewardCommitObserver(accoutStateRepository));
		builder.add(new RemoteObserver(accoutStateRepository));
		builder.add(new OutlinkObserver(accoutStateRepository));
		builder.add(new PruningObserver(accoutStateRepository, nisCache.getTransactionHashCache()));
		builder.add(new TransactionHashesObserver(nisCache.getTransactionHashCache()));
		return builder;
	}
}
