package org.nem.nis.secret;

import org.nem.core.model.observers.*;
import org.nem.nis.cache.MosaicCache;

/**
 * An observer that updates mosaic information.
 */
public class MosaicCreationObserver implements BlockTransactionObserver {
	private final MosaicCache mosaicCache;

	/**
	 * Creates a new observer.
	 *
	 * @param mosaicCache The mosaic cache.
	 */
	public MosaicCreationObserver(final MosaicCache mosaicCache) {
		this.mosaicCache = mosaicCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.MosaicCreation) {
			return;
		}

		this.notify((MosaicCreationNotification)notification, context);
	}

	private void notify(final MosaicCreationNotification notification, final BlockNotificationContext context) {
		if (NotificationTrigger.Execute == context.getTrigger()) {
			this.mosaicCache.add(notification.getMosaic());
		} else {
			this.mosaicCache.remove(notification.getMosaic());
		}
	}
}
