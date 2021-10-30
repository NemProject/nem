package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;

/**
 * An observer that implements BlockTransactionObserver by forwarding all transfer notifications to the wrapped BlockTransferObserver
 * implementation.
 */
public class BlockTransferObserverToBlockTransactionObserverAdapter implements BlockTransactionObserver {
	private final BlockTransferObserver observer;

	/**
	 * Creates a new adapter.
	 *
	 * @param observer The wrapped block transfer observer.
	 */
	public BlockTransferObserverToBlockTransactionObserverAdapter(final BlockTransferObserver observer) {
		this.observer = observer;
	}

	@Override
	public String getName() {
		return this.observer.getName();
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		switch (notification.getType()) {
			case BalanceTransfer:
				this.notifyBlockTransfer((BalanceTransferNotification) notification, context);
				break;

			case BalanceCredit:
				this.notifyCredit((BalanceAdjustmentNotification) notification, context);
				break;

			case BalanceDebit:
				this.notifyDebit((BalanceAdjustmentNotification) notification, context);
				break;
			default :
				break;
		}
	}

	private void notifyBlockTransfer(final BalanceTransferNotification notification, final BlockNotificationContext context) {
		this.notifyCredit(notification.getRecipient(), notification.getAmount(), context);
		this.notifyDebit(notification.getSender(), notification.getAmount(), context);
	}

	private void notifyCredit(final BalanceAdjustmentNotification notification, final BlockNotificationContext context) {
		this.notifyCredit(notification.getAccount(), notification.getAmount(), context);
	}

	private void notifyCredit(final Account account, final Amount amount, final BlockNotificationContext context) {
		if (NotificationTrigger.Execute == context.getTrigger()) {
			this.observer.notifyReceive(context.getHeight(), account, amount);
		} else {
			this.observer.notifySendUndo(context.getHeight(), account, amount);
		}
	}

	private void notifyDebit(final BalanceAdjustmentNotification notification, final BlockNotificationContext context) {
		this.notifyDebit(notification.getAccount(), notification.getAmount(), context);
	}

	private void notifyDebit(final Account account, final Amount amount, final BlockNotificationContext context) {
		if (NotificationTrigger.Execute == context.getTrigger()) {
			this.observer.notifySend(context.getHeight(), account, amount);
		} else {
			this.observer.notifyReceiveUndo(context.getHeight(), account, amount);
		}
	}
}
