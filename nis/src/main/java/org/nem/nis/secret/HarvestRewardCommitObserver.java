package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.*;
import org.nem.nis.poi.*;

/**
 * BlockTransactionObserver that commits harvest rewards to accounts.
 */
public class HarvestRewardCommitObserver implements BlockTransactionObserver {
	private final PoiFacade poiFacade;
	private final AccountCache accountCache;

	/**
	 * Creates a new observer.
	 *
	 * @param poiFacade The poi facade.
	 * @param accountCache The account cache.
	 */
	public HarvestRewardCommitObserver(
			final PoiFacade poiFacade,
			final AccountCache accountCache) {
		this.poiFacade = poiFacade;
		this.accountCache = accountCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.HarvestReward != notification.getType()) {
			return;
		}

		this.notify((BalanceAdjustmentNotification)notification, context);
	}

	public void notify(final BalanceAdjustmentNotification notification, final BlockNotificationContext context) {
		final Address address = notification.getAccount().getAddress();
		final PoiAccountState poiAccountState = this.poiFacade.findForwardedStateByAddress(address, context.getHeight());

		final Account endowed = poiAccountState.getAddress().equals(address)
				? notification.getAccount()
				: this.accountCache.findByAddress(poiAccountState.getAddress());
		if (NotificationTrigger.Execute == context.getTrigger()) {
			endowed.incrementForagedBlocks();
			endowed.incrementBalance(notification.getAmount());
		}
		else {
			endowed.decrementForagedBlocks();
			endowed.decrementBalance(notification.getAmount());
		}
	}
}
