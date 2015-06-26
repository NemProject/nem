package org.nem.nis.controller.requests;

import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.AccountId;
import org.nem.core.utils.StringUtils;

/**
 * View model requests of account-related namespace information.
 */
public class AccountNamespacePage extends AccountId {
	final NamespaceId parent;

	/**
	 * Creates a new account page.
	 *
	 * @param address The address.
	 * @param parent The parent namespace full qualified name.
	 */
	public AccountNamespacePage(final String address, final String parent) {
		super(address);
		this.parent = StringUtils.isNullOrEmpty(parent) ? null : new NamespaceId(parent);
	}

	/**
	 * Gets the parent namespace id.
	 *
	 * @return The parent namespace id.
	 */
	public NamespaceId getParent() {
		return this.parent;
	}
}
