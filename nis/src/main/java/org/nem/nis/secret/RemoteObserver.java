package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

/**
 * A block transaction observer that updates remote account associations.
 */
public class RemoteObserver implements BlockTransactionObserver {
	private final AccountStateCache accountStateCache;

	/**
	 * Creates a new observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public RemoteObserver(final AccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	private RemoteLinks getRemoteLinks(final Account account) {
		return this.accountStateCache.findStateByAddress(account.getAddress()).getRemoteLinks();
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.ImportanceTransfer) {
			return;
		}

		this.notify((ImportanceTransferNotification)notification, context);
	}

	private void notify(final ImportanceTransferNotification notification, final BlockNotificationContext context) {
		final RemoteLink lessorLink = new RemoteLink(
				notification.getLessee().getAddress(),
				context.getHeight(),
				notification.getMode(),
				RemoteLink.Owner.HarvestingRemotely);
		final RemoteLink lesseeLink = new RemoteLink(
				notification.getLessor().getAddress(),
				context.getHeight(),
				notification.getMode(),
				RemoteLink.Owner.RemoteHarvester);

		if (context.getTrigger() == NotificationTrigger.Execute) {
			this.getRemoteLinks(notification.getLessor()).addLink(lessorLink);
			this.getRemoteLinks(notification.getLessee()).addLink(lesseeLink);
		} else {
			this.getRemoteLinks(notification.getLessor()).removeLink(lessorLink);
			this.getRemoteLinks(notification.getLessee()).removeLink(lesseeLink);
		}
	}
}
