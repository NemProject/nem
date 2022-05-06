package org.nem.nis.secret;

import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.NamespaceCache;
import org.nem.nis.state.Mosaics;

/**
 * An observer that updates mosaic definition information.
 */
public class MosaicDefinitionCreationObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public MosaicDefinitionCreationObserver(final NamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.MosaicDefinitionCreation) {
			return;
		}

		this.notify((MosaicDefinitionCreationNotification) notification, context);
	}

	private void notify(final MosaicDefinitionCreationNotification notification, final BlockNotificationContext context) {
		final MosaicDefinition mosaicDefinition = notification.getMosaicDefinition();
		final Mosaics mosaics = this.namespaceCache.get(mosaicDefinition.getId().getNamespaceId()).getMosaics();
		if (NotificationTrigger.Execute == context.getTrigger()) {
			mosaics.add(mosaicDefinition, context.getHeight());
		} else {
			mosaics.remove(mosaicDefinition.getId());
		}
	}
}
