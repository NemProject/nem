package org.nem.nis.service;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.secret.*;
import org.nem.nis.state.ReadOnlyAccountState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.*;

/**
 * Service for executing blocks.
 */
@Service
public class BlockExecutor {
	private final ReadOnlyNisCache nisCache;

	/**
	 * Creates a new block executor.
	 *
	 * @param nisCache The NIS cache.
	 */
	@Autowired(required = true)
	public BlockExecutor(final ReadOnlyNisCache nisCache) {
		this.nisCache = nisCache;
	}

	//region execute

	/**
	 * Executes all transactions in the block with a custom observer.
	 *
	 * @param block The block.
	 * @param observer The observer.
	 */
	public void execute(final Block block, final BlockTransactionObserver observer) {
		final NotificationTrigger trigger = NotificationTrigger.Execute;
		final TransactionObserver transactionObserver = this.createTransactionObserver(block, trigger, observer);

		for (final Transaction transaction : block.getTransactions()) {
			transaction.execute(transactionObserver);
		}

		this.notifyBlockHarvested(transactionObserver, block, trigger);
		this.notifyTransactionHashes(transactionObserver, block);
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
		final NotificationTrigger trigger = NotificationTrigger.Undo;
		final TransactionObserver transactionObserver = this.createTransactionObserver(block, trigger, observer);

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
		// TODO: not sure yet if we should take all the child transactions
		// TODO: 20150103 - i don;t think we need this since MultisigTransaction is explicitly calling transfer on the other (child) transaction
		final List<HashMetaDataPair> pairs =
				Stream.concat(block.getTransactions().stream(), block.getTransactions().stream().flatMap(t -> t.getChildTransactions().stream()))
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
