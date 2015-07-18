package org.nem.nis.secret;

import org.nem.core.model.observers.*;
import org.nem.nis.cache.NamespaceCache;
import org.nem.nis.state.*;

/**
 * An observer that updates mosaic information.
 */
public class MosaicCreationObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public MosaicCreationObserver(final NamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.MosaicCreation) {
			return;
		}

		this.notify((MosaicCreationNotification)notification, context);
	}

	private void notify(final MosaicCreationNotification notification, final BlockNotificationContext context) {
		final Mosaics mosaics = this.namespaceCache.get(notification.getMosaic().getId().getNamespaceId()).getMosaics();
		if (NotificationTrigger.Execute == context.getTrigger()) {
			mosaics.add(notification.getMosaic());
		} else {
			mosaics.remove(notification.getMosaic().getId());
		}
	}
}
