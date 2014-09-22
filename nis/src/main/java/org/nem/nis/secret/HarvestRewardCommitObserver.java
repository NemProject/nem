package org.nem.nis.secret;

import org.nem.core.model.observers.*;

/**
 * BlockTransactionObserver that commits harvest rewards to accounts.
 */
public class HarvestRewardCommitObserver implements BlockTransactionObserver {

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.HarvestReward != notification.getType()) {
			return;
		}

		this.notify((BalanceAdjustmentNotification)notification, context);
	}

	public void notify(final BalanceAdjustmentNotification notification, final BlockNotificationContext context) {
		if (NotificationTrigger.Execute == context.getTrigger()) {
			notification.getAccount().incrementForagedBlocks();
		}
		else {
			notification.getAccount().decrementForagedBlocks();
		}
	}
}
