package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountInfo;

/**
 * Transfer observer that commits balance changes to the underlying accounts.
 */
public class BalanceCommitTransferObserver implements TransactionObserver {
	private final AccountStateCache accountStateCache;

	/**
	 * Creates an observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public BalanceCommitTransferObserver(final AccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public void notify(final Notification notification) {
		switch (notification.getType()) {
			case BalanceTransfer:
				this.notify((BalanceTransferNotification) notification);
				break;

			case BalanceCredit:
			case BalanceDebit:
				this.notify((BalanceAdjustmentNotification) notification);
				break;
			default :
				break;
		}
	}

	private void notify(final BalanceTransferNotification notification) {
		this.notifyDebit(notification.getSender(), notification.getAmount());
		this.notifyCredit(notification.getRecipient(), notification.getAmount());
	}

	private void notify(final BalanceAdjustmentNotification notification) {
		if (NotificationType.BalanceCredit == notification.getType()) {
			this.notifyCredit(notification.getAccount(), notification.getAmount());
		} else {
			this.notifyDebit(notification.getAccount(), notification.getAmount());
		}
	}

	private void notifyCredit(final Account account, final Amount amount) {
		this.getAccountInfo(account).incrementBalance(amount);
	}

	private void notifyDebit(final Account account, final Amount amount) {
		this.getAccountInfo(account).decrementBalance(amount);
	}

	private AccountInfo getAccountInfo(final Account account) {
		return this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo();
	}
}
