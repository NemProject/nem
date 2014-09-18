package org.nem.nis.service;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.BlockScorer;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.secret.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for executing blocks.
 */
@Service
public class BlockExecutor {
	private final PoiFacade poiFacade;
	private final AccountCache accountCache	;

	/**
	 * Creates a new block executor.
	 *
	 * @param poiFacade The poi facade.
	 * @param accountCache The account cache.
	 */
	@Autowired(required = true)
	public BlockExecutor(final PoiFacade poiFacade, final AccountCache accountCache) {
		this.poiFacade = poiFacade;
		this.accountCache = accountCache;
	}
	//region execute

	/**
	 * Executes all transactions in the block.
	 *
	 * @param block The block.
	 */
	public void execute(final Block block) {
		this.execute(block, new ArrayList<>());
	}

	/**
	 * Executes all transactions in the block with a custom observer.
	 *
	 * @param block The block.
	 * @param observer The observer.
	 */
	public void execute(final Block block, final BlockTransactionObserver observer) {
		this.execute(block, Arrays.asList(observer));
	}

	/**
	 * Executes all transactions in the block with custom observers.
	 *
	 * @param block The block.
	 * @param observers The observers.
	 */
	public void execute(final Block block, final Collection<BlockTransactionObserver> observers) {
		final TransactionObserver observer = this.createTransferObserver(block, true, observers);

		for (final Transaction transaction : block.getTransactions()) {
			transaction.execute();
			transaction.execute(observer);
		}

		// TODO 20140909 J-G: can we test this?
		// G-J: I hope we can ;-)  I will add tests.
		final ImportanceTransferObserver itObserver = this.createImportanceTransferObserver(block, true);
		for (final Transaction transaction : block.getTransactions()) {
			if (transaction.getType() == TransactionTypes.IMPORTANCE_TRANSFER) {
				final ImportanceTransferTransaction tx = (ImportanceTransferTransaction)transaction;
				// TODO 20140909 J-G: it seems like you're explicitly calling notifyTransfer here, so do you really need the "phantom" zero transaction?
				// TODO 20140909 J-G: actually, does the phantom transaction allow us to bypass this altogether?
				// G-J no, the phantom transaction is made to trigger AccountsHeightObserver and this is supposed to call RemoteObserver,
				// which doesn't call tx.notifyTransfer()
				itObserver.notifyTransfer(tx.getSigner(), tx.getRemote(), tx.getDirection());
			}
		}

		final Account signer = block.getSigner();
		final PoiAccountState poiAccountState = BlockScorer.getForwardedAccountState(this.poiFacade, signer.getAddress(), block.getHeight());
		final Account endowed = poiAccountState.getAddress().equals(signer.getAddress()) ? signer : this.accountCache.findByAddress(poiAccountState.getAddress());
		endowed.incrementForagedBlocks();
		endowed.incrementBalance(block.getTotalFee());
		observer.notifyCredit(endowed, block.getTotalFee());
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceCredit, block.getSigner(), block.getTotalFee()));
	}

	//endregion

	//region undo

	/**
	 * Undoes all transactions in the block.
	 *
	 * @param block The block.
	 */
	public void undo(final Block block) {
		this.undo(block, new ArrayList<>());
	}

	/**
	 * Undoes all transactions in the block with a custom observer.
	 *
	 * @param block The block.
	 * @param observer The observer.
	 */
	public void undo(final Block block, final BlockTransactionObserver observer) {
		this.undo(block, Arrays.asList(observer));
	}

	/**
	 * Undoes all transactions in the block with custom observers.
	 *
	 * @param block The block.
	 * @param observers The observers.
	 */
	public void undo(final Block block, final Collection<BlockTransactionObserver> observers) {
		final TransactionObserver observer = this.createTransferObserver(block, false, observers);

		final Account signer = block.getSigner();
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, block.getSigner(), block.getTotalFee()));
		signer.decrementForagedBlocks();
		signer.decrementBalance(block.getTotalFee());

		final ImportanceTransferObserver itObserver = this.createImportanceTransferObserver(block, true);
		for (final Transaction transaction : getReverseTransactions(block)) {
			if (transaction.getType() == TransactionTypes.IMPORTANCE_TRANSFER) {
				final ImportanceTransferTransaction tx = (ImportanceTransferTransaction)transaction;
				itObserver.notifyTransfer(tx.getSigner(), tx.getRemote(), tx.getDirection());
			}
		}

		for (final Transaction transaction : getReverseTransactions(block)) {
			transaction.undo(observer);
			transaction.undo();
		}
	}

	//endregion undo

	private static Iterable<Transaction> getReverseTransactions(final Block block) {
		return () -> new ReverseListIterator<>(block.getTransactions());
	}

	private ImportanceTransferObserver createImportanceTransferObserver(
			final Block block,
			final boolean isExecute) {

		return new RemoteObserver(this.poiFacade, block.getHeight(), isExecute);
	}
	
	private TransactionObserver createTransferObserver(
			final Block block,
			final boolean isExecute,
			final Collection<BlockTransactionObserver> observers) {
		final AggregateBlockTransactionObserverBuilder btoBuilder = new AggregateBlockTransactionObserverBuilder();
		btoBuilder.add(new WeightedBalancesObserver(this.poiFacade));
		observers.forEach(obj -> btoBuilder.add((obj)));

		final TransactionObserver aggregateObserver = new BlockTransactionObserverToTransactionObserverAdapter(
				btoBuilder.build(),
				new BlockNotificationContext(block.getHeight(), isExecute ? NotificationTrigger.Execute : NotificationTrigger.Undo));
		final TransactionObserver outlinkObserver = new TransferObserverToTransactionObserverAdapter(
				new OutlinkObserver(this.poiFacade, block.getHeight(), isExecute));

		// in an undo operation, the OutlinkObserver should be run before the balance is updated
		// (so that the matching link can be found and removed)
		final List<TransactionObserver> transactionObservers = Arrays.asList(aggregateObserver, outlinkObserver);
		if (!isExecute) {
			Collections.reverse(transactionObservers);
		}

		final AggregateTransactionObserverBuilder toBuilder = new AggregateTransactionObserverBuilder();
		transactionObservers.forEach(obj -> toBuilder.add(obj));
		return toBuilder.build();
	}
}
