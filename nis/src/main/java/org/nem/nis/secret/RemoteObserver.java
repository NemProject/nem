package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;

/**
 * A block transaction observer that updates remote account associations.
 */
public class RemoteObserver implements BlockTransactionObserver {
	private final AccountStateCache accountStateCache;
	private final BlockHeight mosaicRedefinitionForkHeight;

	/**
	 * Creates a new observer.
	 *
	 * @param accountStateCache The account state cache.
	 * @param mosaicRedefinitionForkHeight The mosaic redefinition fork height.
	 */
	public RemoteObserver(final AccountStateCache accountStateCache, final BlockHeight mosaicRedefinitionForkHeight) {
		this.accountStateCache = accountStateCache;
		this.mosaicRedefinitionForkHeight = mosaicRedefinitionForkHeight;
	}

	private RemoteLinks getRemoteLinks(final Address address) {
		return this.accountStateCache.findStateByAddress(address).getRemoteLinks();
	}

	private RemoteLinks getRemoteLinks(final Account account) {
		return this.getRemoteLinks(account.getAddress());
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.ImportanceTransfer) {
			return;
		}

		this.notify((ImportanceTransferNotification) notification, context);
	}

	private void notify(final ImportanceTransferNotification notification, final BlockNotificationContext context) {
		final Address remoteAddress = ImportanceTransferMode.Activate == notification.getMode()
				? notification.getLessee().getAddress()
				: context.getHeight().getRaw() < this.mosaicRedefinitionForkHeight.getRaw()
						? notification.getLessee().getAddress()
						: this.getRemoteLinks(notification.getLessor()).getCurrent().getLinkedAddress();

		final RemoteLink lessorLink = new RemoteLink(remoteAddress, context.getHeight(), notification.getMode(),
				RemoteLink.Owner.HarvestingRemotely);
		final RemoteLink lesseeLink = new RemoteLink(notification.getLessor().getAddress(), context.getHeight(), notification.getMode(),
				RemoteLink.Owner.RemoteHarvester);

		if (context.getTrigger() == NotificationTrigger.Execute) {
			this.getRemoteLinks(notification.getLessor()).addLink(lessorLink);
			this.getRemoteLinks(remoteAddress).addLink(lesseeLink);
		} else {
			this.getRemoteLinks(notification.getLessor()).removeLink(lessorLink);
			this.getRemoteLinks(remoteAddress).removeLink(lesseeLink);
		}
	}
}
