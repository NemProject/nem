package org.nem.core.model.observers;

/**
 * An observer that implements TransactionObserver by forwarding all transfer notifications
 * to the wrapped TransferObserver implementation.
 */
public class TransferObserverToTransactionObserverAdapter implements TransactionObserver {
	private final TransferObserver observer;

	/**
	 * Creates a new adapter.
	 *
	 * @param observer The wrapped transfer observer.
	 */
	public TransferObserverToTransactionObserverAdapter(final TransferObserver observer) {
		this.observer = observer;
	}

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
		this.observer.notifyTransfer(notification.getSender(), notification.getRecipient(), notification.getAmount());
	}

	private void notifyCredit(final BalanceAdjustmentNotification notification) {
		this.observer.notifyCredit(notification.getAccount(), notification.getAmount());
	}

	private void notifyDebit(final BalanceAdjustmentNotification notification) {
		this.observer.notifyDebit(notification.getAccount(), notification.getAmount());
	}
}
