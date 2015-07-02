package org.nem.nis.secret;

import org.nem.core.model.observers.MosaicCreationNotification;
import org.nem.core.model.observers.Notification;
import org.nem.core.model.observers.NotificationType;
import org.nem.nis.cache.MosaicCache;

public class MosaicCreationObserver implements BlockTransactionObserver {
	private final MosaicCache mosaicCache;

	public MosaicCreationObserver(MosaicCache mosaicCache) {
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
