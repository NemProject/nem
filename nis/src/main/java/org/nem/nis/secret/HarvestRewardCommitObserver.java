package org.nem.nis.secret;

import org.nem.core.model.observers.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountInfo;

/**
 * BlockTransactionObserver that commits harvest rewards to accounts.
 */
public class HarvestRewardCommitObserver implements BlockTransactionObserver {
	private final AccountStateRepository accountStateRepository;

	/**
	 * Creates an observer.
	 *
	 * @param accountStateRepository The poi facade.
	 */
	public HarvestRewardCommitObserver(final AccountStateRepository accountStateRepository) {
		this.accountStateRepository = accountStateRepository;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.HarvestReward != notification.getType()) {
			return;
		}

		this.notify((BalanceAdjustmentNotification)notification, context);
	}

	private void notify(final BalanceAdjustmentNotification notification, final BlockNotificationContext context) {
		final AccountInfo accountInfo = this.accountStateRepository.findStateByAddress(notification.getAccount().getAddress()).getAccountInfo();
		if (NotificationTrigger.Execute == context.getTrigger()) {
			accountInfo.incrementHarvestedBlocks();
		} else {
			accountInfo.decrementHarvestedBlocks();
		}
	}
}
