package org.nem.core.model.observers;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;

/**
 * An observer that notifies listeners when transfers are made.
 * This abstract class adapts the new TransactionObserver interface to the original TransferObserver interface.
 */
public abstract class TransferObserver implements TransactionObserver {

	/**
	 * The sender has transferred the specified amount to the recipient.
	 *
	 * @param sender The sender.
	 * @param recipient The recipient.
	 * @param amount The amount.
	 */
	public abstract void notifyTransfer(final Account sender, final Account recipient, final Amount amount);

	/**
	 * The account has been credited the specified amount.
	 *
	 * @param account The account.
	 * @param amount The amount.
	 */
	public abstract void notifyCredit(final Account account, final Amount amount);

	/**
	 * The account has been debited the specified amount.
	 *
	 * @param account The account.
	 * @param amount The amount.
	 */
	public abstract void notifyDebit(final Account account, final Amount amount);

	@Override
	public final void notify(final Notification notification) {
		switch (notification.getType()) {
			case BalanceTransfer:
				this.notifyTransfer((BalanceTransferNotification)notification);
				break;

			case BalanceCredit:
				this.notifyCredit((BalanceAdjustmentNotification)notification);
				break;

			case BalanceDebit:
				this.notifyDebit((BalanceAdjustmentNotification)notification);
				break;
		}
	}

	private void notifyTransfer(final BalanceTransferNotification notification) {
		this.notifyTransfer(notification.getSender(), notification.getRecipient(), notification.getAmount());
	}

	private void notifyCredit(final BalanceAdjustmentNotification notification) {
		this.notifyCredit(notification.getAccount(), notification.getAmount());
	}

	private void notifyDebit(final BalanceAdjustmentNotification notification) {
		this.notifyDebit(notification.getAccount(), notification.getAmount());
	}
}