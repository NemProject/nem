package org.nem.nis.secret;

import org.nem.core.model.observers.*;
import org.nem.nis.poi.*;

/**
 * BlockTransactionObserver that commits harvest rewards to accounts.
 */
public class HarvestRewardCommitObserver implements BlockTransactionObserver {
	private final PoiFacade poiFacade;

	/**
	 * Creates an observer.
	 *
	 * @param poiFacade The poi facade.
	 */
	public HarvestRewardCommitObserver(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.HarvestReward != notification.getType()) {
			return;
		}

		this.notify((BalanceAdjustmentNotification)notification, context);
	}

	public void notify(final BalanceAdjustmentNotification notification, final BlockNotificationContext context) {
		final AccountInfo accountInfo = this.poiFacade.findStateByAddress(notification.getAccount().getAddress()).getAccountInfo();
		if (NotificationTrigger.Execute == context.getTrigger()) {
			accountInfo.incrementHarvestedBlocks();
		} else {
			accountInfo.decrementHarvestedBlocks();
		}
	}
}
