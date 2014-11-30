package org.nem.nis.service;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.AccountCache;
import org.nem.nis.poi.*;
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
	private final AccountCache accountCache;

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
		// in order for all the downstream observers to behave correctly (without needing to know about remote foraging)
		// trigger harvest notifications with the forwarded account (where available) instead of the remote account
		final Address address = block.getSigner().getAddress();
		final PoiAccountState poiAccountState = this.poiFacade.findForwardedStateByAddress(address, block.getHeight());

		final Account endowed = poiAccountState.getAddress().equals(address)
				? block.getSigner()
				: this.accountCache.findByAddress(poiAccountState.getAddress());

		final List<NotificationType> types = NotificationTrigger.Execute == trigger
				? Arrays.asList(NotificationType.BalanceCredit, NotificationType.HarvestReward)
				: Arrays.asList(NotificationType.HarvestReward, NotificationType.BalanceDebit);

		for (final NotificationType type : types) {
			observer.notify(new BalanceAdjustmentNotification(type, endowed, block.getTotalFee()));
		}
	}

	private void notifyTransactionHashes(final TransactionObserver observer, final Block block) {
		final List<HashTimeInstantPair> pairs = new ArrayList<>();
		block.getTransactions().stream().forEach(t -> pairs.add(new HashTimeInstantPair(HashUtils.calculateHash(t), t.getTimeStamp())));
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
