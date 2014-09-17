package org.nem.core.model.observers;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;

/**
 * An observer that implements TransferObserver by forwarding all transfer notifications
 * to the wrapped TransactionObserver implementation.
 */
public class TransferObserverToTransactionObserverAdapter implements TransferObserver {
	private final TransactionObserver observer;

	/**
	 * Creates a new adapter.
	 *
	 * @param observer The wrapped transaction observer.
	 */
	public TransferObserverToTransactionObserverAdapter(final TransactionObserver observer) {
		this.observer = observer;
	}

	@Override
	public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
		this.observer.notify(new BalanceTransferNotification(sender, recipient, amount));
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
		this.observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, amount));
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
		this.observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, account, amount));
	}
}
