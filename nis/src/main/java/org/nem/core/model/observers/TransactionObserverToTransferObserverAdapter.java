package org.nem.core.model.observers;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;

/**
 * An observer that implements TransactionObserver by forwarding all transfer notifications
 * to the derived TransferObserver implementation.
 */
public abstract class TransactionObserverToTransferObserverAdapter implements TransferObserver, TransactionObserver {

	@Override
	public abstract void notifyTransfer(final Account sender, final Account recipient, final Amount amount);

	@Override
	public abstract void notifyCredit(final Account account, final Amount amount);

	@Override
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
