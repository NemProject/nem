package org.nem.nis.service;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.secret.*;
import org.springframework.stereotype.Service;

/**
 * Service for executing blocks.
 */
@Service
public class BlockExecutor {

	//region execute

	/**
	 * Executes all transactions in the block with a custom observer.
	 *
	 * @param block The block.
	 * @param observer The observer.
	 */
	public void execute(final Block block, final BlockTransactionObserver observer) {
		final TransactionObserver transactionObserver = this.createTransactionObserver(block, NotificationTrigger.Execute, observer);

		for (final Transaction transaction : block.getTransactions()) {
			transaction.execute(transactionObserver);
		}

		transactionObserver.notify(createHarvestRewardNotification(block));
	}

	//endregion

	//region undo

	/**
	 * Undoes all transactions in the block with a custom observer.
	 *
	 * @param block The block.
	 * @param observer The observer.
	 */
	public void undo(final Block block, final BlockTransactionObserver observer) {
		final TransactionObserver transactionObserver = this.createTransactionObserver(block, NotificationTrigger.Undo, observer);

		transactionObserver.notify(createHarvestRewardNotification(block));
		for (final Transaction transaction : getReverseTransactions(block)) {
			transaction.undo(transactionObserver);
		}
	}

	//endregion undo

	private static Iterable<Transaction> getReverseTransactions(final Block block) {
		return () -> new ReverseListIterator<>(block.getTransactions());
	}

	private TransactionObserver createTransactionObserver(
			final Block block,
			final NotificationTrigger trigger,
			final BlockTransactionObserver observer) {
		final BlockNotificationContext context = new BlockNotificationContext(block.getHeight(), trigger);
		return new BlockTransactionObserverToTransactionObserverAdapter(observer, context);
	}

	private static Notification createHarvestRewardNotification(final Block block) {
		return new BalanceAdjustmentNotification(NotificationType.HarvestReward, block.getSigner(), block.getTotalFee());
	}
}
