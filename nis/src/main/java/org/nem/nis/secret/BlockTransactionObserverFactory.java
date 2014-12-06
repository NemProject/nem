package org.nem.nis.secret;

import org.nem.nis.NisCache;
import org.nem.core.model.observers.BalanceCommitTransferObserver;
import org.nem.nis.AccountsHeightObserver;
import org.nem.nis.poi.PoiFacade;

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
	public BlockTransactionObserver createUndoCommitObserver(final NisCache nisCache){
		return createBuilder(nisCache).buildReverse();
	}

	private static AggregateBlockTransactionObserverBuilder createBuilder(final NisCache nisCache){
		final PoiFacade poiFacade = nisCache.getAccountAnalyzer().getPoiFacade();
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();
		builder.add(new WeightedBalancesObserver(poiFacade));
		builder.add(new AccountsHeightObserver(nisCache.getAccountAnalyzer()));
		builder.add(new BalanceCommitTransferObserver());
		builder.add(new HarvestRewardCommitObserver());
		builder.add(new RemoteObserver(poiFacade));
		builder.add(new OutlinkObserver(poiFacade));
		builder.add(new PruningObserver(poiFacade, nisCache.getTransactionHashCache()));
		builder.add(new TransactionHashesObserver(nisCache.getTransactionHashCache()));
		return builder;
	}
}
