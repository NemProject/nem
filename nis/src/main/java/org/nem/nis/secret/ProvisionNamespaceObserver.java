package org.nem.nis.secret;

import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.NamespaceCache;

/**
 * An observer that updates namespace information.
 */
public class ProvisionNamespaceObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public ProvisionNamespaceObserver(final NamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.ProvisionNamespace) {
			return;
		}

		this.notify((ProvisionNamespaceNotification)notification, context);
	}

	private void notify(final ProvisionNamespaceNotification notification, final BlockNotificationContext context) {
		if (NotificationTrigger.Execute == context.getTrigger()) {
			final Namespace namespace = new Namespace(
					notification.getNamespaceId(),
					notification.getOwner(),
					context.getHeight());
			this.namespaceCache.add(namespace);
		} else {
			this.namespaceCache.remove(notification.getNamespaceId());
		}
	}
}
