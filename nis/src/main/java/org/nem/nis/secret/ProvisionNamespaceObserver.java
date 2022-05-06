package org.nem.nis.secret;

import org.nem.core.model.namespace.*;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.*;

import java.util.*;

/**
 * An observer that updates namespace information.
 */
public class ProvisionNamespaceObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;
	private final AccountStateCache accountStateCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public ProvisionNamespaceObserver(final NamespaceCache namespaceCache, final AccountStateCache accountStateCache) {
		this.namespaceCache = namespaceCache;
		this.accountStateCache = accountStateCache;
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
			this.namespaceCache.add(namespace);
			if (namespace.getId().isRoot()) {
				this.updateAccountStates(namespace.getId());
			}
		} else {
			this.namespaceCache.remove(notification.getNamespaceId());
		}
	}

	private void updateAccountStates(final NamespaceId namespaceId) {
		final Collection<NamespaceId> ids = new ArrayList<>();
		ids.add(namespaceId);
		ids.addAll(this.namespaceCache.getSubNamespaceIds(namespaceId));
		ids.forEach(id -> {
			NamespaceCacheUtils.getMosaicIds(this.namespaceCache, id).forEach(mosaicId -> {
				NamespaceCacheUtils.getMosaicOwners(this.namespaceCache, mosaicId)
						.forEach(owner -> this.accountStateCache.findStateByAddress(owner).getAccountInfo().addMosaicId(mosaicId));
			});
		});
	}
}
