package org.nem.core.model.observers;

import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;

/**
 * A notification that a namespace has been provisioned.
 */
public class ProvisionNamespaceNotification extends Notification {
	private final Account owner;
	private final NamespaceId namespaceId;

	/**
	 * Creates a new provision namespace notification.
	 *
	 * @param owner The owner account.
	 * @param namespaceId The namespace id.
	 */
	public ProvisionNamespaceNotification(final Account owner, final NamespaceId namespaceId) {
		super(NotificationType.ProvisionNamespace);
		this.owner = owner;
		this.namespaceId = namespaceId;
	}

	/**
	 * Gets the owner account.
	 *
	 * @return The owner account.
	 */
	public Account getOwner() {
		return this.owner;
	}

	/**
	 * Gets the namespace id.
	 *
	 * @return The namespace id.
	 */
	public NamespaceId getNamespaceId() {
		return this.namespaceId;
	}
}
