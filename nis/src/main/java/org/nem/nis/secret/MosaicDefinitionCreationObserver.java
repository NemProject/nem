package org.nem.nis.secret;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

/**
 * An observer that updates mosaic definition information.
 */
public class MosaicDefinitionCreationObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;
	private final ExpiredMosaicCache expiredMosaicCache;
	private final BlockHeight mosaicRedefinitionForkHeight;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache Namespace cache.
	 * @param expiredMosaicCache Expired mosaic cache.
	 * @param mosaicRedefinitionForkHeight Mosaic redefinition fork height.
	 */
	public MosaicDefinitionCreationObserver(final NamespaceCache namespaceCache, final ExpiredMosaicCache expiredMosaicCache,
			final BlockHeight mosaicRedefinitionForkHeight) {
		this.namespaceCache = namespaceCache;
		this.expiredMosaicCache = expiredMosaicCache;
		this.mosaicRedefinitionForkHeight = mosaicRedefinitionForkHeight;
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
		final MosaicId mosaicId = mosaicDefinition.getId();

		final Mosaics mosaics = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics();
		if (NotificationTrigger.Execute == context.getTrigger()) {
			if (context.getHeight().compareTo(this.mosaicRedefinitionForkHeight) < 0) {
				final MosaicEntry mosaicEntry = mosaics.get(mosaicId);
				if (null != mosaicEntry) {
					this.expiredMosaicCache.addExpiration(context.getHeight(), mosaicId, mosaicEntry.getBalances(),
							ExpiredMosaicType.Expired);
				}
			}

			mosaics.add(mosaicDefinition, context.getHeight());
		} else {
			mosaics.remove(mosaicId);

			if (context.getHeight().compareTo(this.mosaicRedefinitionForkHeight) < 0) {
				this.expiredMosaicCache.removeExpiration(context.getHeight(), mosaicId);
			}
		}
	}
}
