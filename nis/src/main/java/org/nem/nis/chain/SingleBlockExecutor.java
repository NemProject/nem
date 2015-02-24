package org.nem.nis.chain;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.secret.*;
import org.nem.nis.state.ReadOnlyAccountState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for executing a single block in a single direction.
 * TODO 20150221 J-J: add tests for this class
 */
public class SingleBlockExecutor {
	private final ReadOnlyNisCache nisCache;
	private final Block block;
	private final TransactionObserver executeObserver;
	private final TransactionObserver undoObserver;

	/**
	 * Creates a new block executor.
	 *
	 * @param nisCache The NIS cache.
	 * @param observer The block observer.
	 * @param block The block.
	 */
	public SingleBlockExecutor(
			final ReadOnlyNisCache nisCache,
			final BlockTransactionObserver observer,
			final Block block) {
		this.nisCache = nisCache;
		this.block = block;
		this.executeObserver = this.createTransactionObserver(block, NotificationTrigger.Execute, observer);
		this.undoObserver = this.createTransactionObserver(block, NotificationTrigger.Undo, observer);
	}

	//region execute

	/**
	 * Executes the block.
	 */
	public void execute() {
		final NotificationTrigger trigger = NotificationTrigger.Execute;
		this.notifyBlockHarvested(this.executeObserver, this.block, trigger);
		this.notifyTransactionHashes(this.executeObserver, this.block);
	}

	/**
	 * Executes a transaction.
	 *
	 * @param transaction The transaction.
	 */
	public void execute(final Transaction transaction) {
		transaction.execute(this.executeObserver);
	}

	//endregion

	//region undo

	/**
	 * Undoes all transactions in the block.
	 */
	public void undoAll() {
		final NotificationTrigger trigger = NotificationTrigger.Undo;
		final TransactionObserver transactionObserver = this.undoObserver;

		this.notifyBlockHarvested(transactionObserver, block, trigger);
		this.notifyTransactionHashes(transactionObserver, block);
		for (final Transaction transaction : getReverseTransactions(block)) {
			transaction.undo(transactionObserver);
		}
	}

	//endregion undo

	private void notifyBlockHarvested(final TransactionObserver observer, final Block block, final NotificationTrigger trigger) {
		// in order for all the downstream observers to behave correctly (without needing to know about remote harvesting)
		// trigger harvest notifications with the forwarded account (where available) instead of the remote account
		final Address address = block.getSigner().getAddress();
		final ReadOnlyAccountState accountState = this.nisCache.getAccountStateCache().findForwardedStateByAddress(address, block.getHeight());

		final Account endowed = accountState.getAddress().equals(address)
				? block.getSigner()
				: this.nisCache.getAccountCache().findByAddress(accountState.getAddress());

		final List<NotificationType> types = NotificationTrigger.Execute == trigger
				? Arrays.asList(NotificationType.BalanceCredit, NotificationType.BlockHarvest)
				: Arrays.asList(NotificationType.BlockHarvest, NotificationType.BalanceDebit);

		for (final NotificationType type : types) {
			observer.notify(new BalanceAdjustmentNotification(type, endowed, block.getTotalFee()));
		}
	}

	private void notifyTransactionHashes(final TransactionObserver observer, final Block block) {
		final List<HashMetaDataPair> pairs =
				BlockExtensions.streamDefault(block)
						.map(t -> new HashMetaDataPair(HashUtils.calculateHash(t), new HashMetaData(block.getHeight(), t.getTimeStamp())))
						.collect(Collectors.toList());
		observer.notify(new TransactionHashesNotification(pairs));
	}

	private static Iterable<Transaction> getReverseTransactions(final Block block) {
		return () -> new ReverseListIterator<>(block.getTransactions());
	}

	private TransactionObserver createTransactionObserver(
			final Block block,
			final NotificationTrigger trigger,
			final BlockTransactionObserver observer) {
		final BlockNotificationContext context = new BlockNotificationContext(block.getHeight(), block.getTimeStamp(), trigger);
		return new BlockTransactionObserverToTransactionObserverAdapter(observer, context);
	}
}