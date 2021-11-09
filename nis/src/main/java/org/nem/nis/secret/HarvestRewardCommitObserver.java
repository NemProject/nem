package org.nem.nis.secret;

import org.nem.core.model.observers.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountInfo;

/**
 * BlockTransactionObserver that commits harvest rewards to accounts.
 */
public class HarvestRewardCommitObserver implements BlockTransactionObserver {
	private final AccountStateCache accountStateCache;

	/**
	 * Creates an observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public HarvestRewardCommitObserver(final AccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.BlockHarvest != notification.getType()) {
			return;
		}

		this.notify((BalanceAdjustmentNotification) notification, context);
	}

	private void notify(final BalanceAdjustmentNotification notification, final BlockNotificationContext context) {
		final AccountInfo accountInfo = this.accountStateCache.findStateByAddress(notification.getAccount().getAddress()).getAccountInfo();
		if (NotificationTrigger.Execute == context.getTrigger()) {
			accountInfo.incrementHarvestedBlocks();
		} else {
			accountInfo.decrementHarvestedBlocks();
		}
	}
}
