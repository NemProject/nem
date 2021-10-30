package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;

import java.math.BigInteger;

/**
 * A block transaction observer that updates outlink information.
 */
public class OutlinkObserver implements BlockTransactionObserver {
	private final AccountStateCache accountStateCache;

	/**
	 * Creates a new observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public OutlinkObserver(final AccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.BalanceTransfer != notification.getType()) {
			return;
		}

		this.notify((BalanceTransferNotification) notification, NotificationTrigger.Execute == context.getTrigger(), context.getHeight());
	}

	private void notify(final BalanceTransferNotification notification, final boolean isExecute, final BlockHeight height) {
		// Trying to gain importance by sending nem to yourself?
		final Account sender = notification.getSender();
		final Account recipient = notification.getRecipient();
		if (sender.getAddress().equals(recipient.getAddress())) {
			return;
		}

		final Amount linkWeight = this.calculateLinkWeight(height, isExecute ? sender : recipient, notification.getAmount());
		if (isExecute) {
			final AccountLink link = new AccountLink(height, linkWeight, recipient.getAddress());
			this.getState(sender).getImportanceInfo().addOutlink(link);
		} else {
			final AccountLink link = new AccountLink(height, linkWeight, sender.getAddress());
			this.getState(recipient).getImportanceInfo().removeOutlink(link);
		}
	}

	private Amount calculateLinkWeight(final BlockHeight height, final Account sender, final Amount amount) {
		final ReadOnlyWeightedBalances weightedBalances = this.getState(sender).getWeightedBalances();
		final BigInteger vested = BigInteger.valueOf(getNumMicroNem(weightedBalances.getVested(height)));
		final BigInteger unvested = BigInteger.valueOf(getNumMicroNem(weightedBalances.getUnvested(height)));
		if (unvested.compareTo(BigInteger.ZERO) <= 0) {
			return amount;
		}

		// only use the vested portion of an account's balance in outlink determination
		final long rawAdjustedWeight = BigInteger.valueOf(amount.getNumMicroNem()).multiply(vested).divide(vested.add(unvested))
				.longValue();
		return Amount.fromMicroNem(rawAdjustedWeight);
	}

	private AccountState getState(final Account account) {
		return this.accountStateCache.findStateByAddress(account.getAddress());
	}

	private static long getNumMicroNem(final Amount amount) {
		return null == amount ? 0 : amount.getNumMicroNem();
	}
}
