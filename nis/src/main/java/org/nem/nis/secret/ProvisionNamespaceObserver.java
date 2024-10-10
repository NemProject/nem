package org.nem.nis.secret;

import java.util.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

/**
 * An observer that updates namespace information.
 */
public class ProvisionNamespaceObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;
	private final AccountStateCache accountStateCache;
	private final ExpiredMosaicCache expiredMosaicCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache Namespace cache.
	 * @param accountStateCache Account state cache.
	 * @param expiredMosaicCache Expired mosaic cache.
	 */
	public ProvisionNamespaceObserver(final NamespaceCache namespaceCache, final AccountStateCache accountStateCache,
			final ExpiredMosaicCache expiredMosaicCache) {
		this.namespaceCache = namespaceCache;
		this.accountStateCache = accountStateCache;
		this.expiredMosaicCache = expiredMosaicCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.ProvisionNamespace) {
			return;
		}

		this.notify((ProvisionNamespaceNotification) notification, context);
	}

	private void notify(final ProvisionNamespaceNotification notification, final BlockNotificationContext context) {
		if (NotificationTrigger.Execute == context.getTrigger()) {
			final Namespace namespace = new Namespace(notification.getNamespaceId(), notification.getOwner(), context.getHeight());

			// need to check isActive before adding to cache
			final boolean isRoot = namespace.getId().isRoot();
			final boolean isActive = isRoot && this.namespaceCache.isActive(namespace.getId(), context.getHeight());

			this.namespaceCache.add(namespace);

			if (isRoot) {
				this.updateAccountStates(namespace.getId(), context.getHeight(), isActive);
			}
		} else {
			this.namespaceCache.remove(notification.getNamespaceId());
		}
	}

	private void updateAccountStates(final NamespaceId namespaceId, final BlockHeight height, final boolean isActive) {
		final Collection<NamespaceId> ids = new ArrayList<>();
		ids.add(namespaceId);
		ids.addAll(this.namespaceCache.getSubNamespaceIds(namespaceId));
		ids.forEach(id -> {
			NamespaceCacheUtils.getMosaicIds(this.namespaceCache, id).forEach(mosaicId -> {
				final ReadOnlyMosaicEntry mosaicEntry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, mosaicId);
				final ReadOnlyMosaicBalances mosaicBalances = mosaicEntry.getBalances();

				mosaicBalances.getOwners()
						.forEach(owner -> this.accountStateCache.findStateByAddress(owner).getAccountInfo().addMosaicId(mosaicId));

				if (!isActive) {
					this.expiredMosaicCache.addExpiration(height, mosaicId, mosaicBalances, ExpiredMosaicType.Restored);
				}
			});
		});
	}
}
