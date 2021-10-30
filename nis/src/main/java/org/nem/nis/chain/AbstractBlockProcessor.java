package org.nem.nis.chain;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.secret.*;
import org.nem.nis.state.ReadOnlyAccountState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for block processors.
 */
public abstract class AbstractBlockProcessor implements BlockProcessor {
	private final ReadOnlyNisCache nisCache;
	private final Block block;
	private final TransactionObserver observer;
	private final NotificationTrigger trigger;

	/**
	 * Creates a new block processor.
	 *
	 * @param nisCache The NIS cache.
	 * @param block The block.
	 * @param observer The block observer.
	 * @param trigger The notification trigger.
	 */
	protected AbstractBlockProcessor(final ReadOnlyNisCache nisCache, final Block block, final BlockTransactionObserver observer,
			final NotificationTrigger trigger) {
		this.nisCache = nisCache;
		this.block = block;
		this.trigger = trigger;
		this.observer = this.createTransactionObserver(observer);
	}

	/**
	 * Gets the transaction observer.
	 *
	 * @return The observer.
	 */
	protected TransactionObserver getObserver() {
		return this.observer;
	}

	/**
	 * Raises a BlockHarvest notification.
	 */
	protected void notifyBlockHarvested() {
		// in order for all the downstream observers to behave correctly (without needing to know about remote harvesting)
		// trigger harvest notifications with the forwarded account (where available) instead of the remote account
		final Address address = this.block.getSigner().getAddress();
		final ReadOnlyAccountState accountState = this.nisCache.getAccountStateCache().findForwardedStateByAddress(address,
				this.block.getHeight());

		final Account endowed = accountState.getAddress().equals(address)
				? this.block.getSigner()
				: this.nisCache.getAccountCache().findByAddress(accountState.getAddress());

		final List<NotificationType> types = NotificationTrigger.Execute == this.trigger
				? Arrays.asList(NotificationType.BalanceCredit, NotificationType.BlockHarvest)
				: Arrays.asList(NotificationType.BlockHarvest, NotificationType.BalanceDebit);

		for (final NotificationType type : types) {
			this.observer.notify(new BalanceAdjustmentNotification(type, endowed, this.block.getTotalFee()));
		}
	}

	/**
	 * Raises a TransactionHashes notification.
	 */
	protected void notifyTransactionHashes() {
		final List<HashMetaDataPair> pairs = BlockExtensions.streamDefault(this.block)
				.map(t -> new HashMetaDataPair(HashUtils.calculateHash(t), new HashMetaData(this.block.getHeight(), t.getTimeStamp())))
				.collect(Collectors.toList());
		this.observer.notify(new TransactionHashesNotification(pairs));
	}

	private TransactionObserver createTransactionObserver(final BlockTransactionObserver observer) {
		final BlockNotificationContext context = new BlockNotificationContext(this.block.getHeight(), this.block.getTimeStamp(),
				this.trigger);
		return new BlockTransactionObserverToTransactionObserverAdapter(observer, context);
	}
}
