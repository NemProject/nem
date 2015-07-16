package org.nem.core.model.observers;

import java.util.*;

/**
 * A TransactionObserver implementation that gathers all notifications and applies them in reverse order.
 * In addition, balance transfer notifications are automatically reversed.
 */
public class ReverseTransactionObserver implements TransactionObserver {
	private final TransactionObserver observer;
	private final List<Notification> pendingNotifications = new ArrayList<>();

	/**
	 * Creates a new adapter.
	 *
	 * @param observer The wrapped transaction observer.
	 */
	public ReverseTransactionObserver(final TransactionObserver observer) {
		this.observer = observer;
	}

	@Override
	public void notify(final Notification notification) {
		this.pendingNotifications.add(reverse(notification));
	}

	/**
	 * Commits all notifications by replaying them in reverse order.
	 */
	public void commit() {
		// apply the transfers in reverse order because order might be important for some observers
		for (int i = this.pendingNotifications.size() - 1; i >= 0; --i) {
			this.observer.notify(this.pendingNotifications.get(i));
		}
	}

	private static Notification reverse(final Notification notification) {
		switch (notification.getType()) {
			case BalanceTransfer:
				return swapAccounts((BalanceTransferNotification)notification);

			case SmartTileTransfer:
				return swapAccounts((SmartTileTransferNotification)notification);

			case BalanceCredit:
				return changeType((BalanceAdjustmentNotification)notification, NotificationType.BalanceDebit);

			case BalanceDebit:
				return changeType((BalanceAdjustmentNotification)notification, NotificationType.BalanceCredit);

			default:
				return notification;
		}
	}

	private static Notification swapAccounts(final BalanceTransferNotification notification) {
		return new BalanceTransferNotification(notification.getRecipient(), notification.getSender(), notification.getAmount());
	}

	private static Notification swapAccounts(final SmartTileTransferNotification notification) {
		return new SmartTileTransferNotification(notification.getRecipient(), notification.getSender(), notification.getSmartTile());
	}

	private static Notification changeType(final BalanceAdjustmentNotification notification, final NotificationType type) {
		return new BalanceAdjustmentNotification(type, notification.getAccount(), notification.getAmount());
	}
}
