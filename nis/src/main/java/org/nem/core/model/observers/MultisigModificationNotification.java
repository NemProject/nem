package org.nem.core.model.observers;

import org.nem.core.model.*;

import java.util.*;

/**
 * A notification that cosignatory modification has occured.
 */
public class MultisigModificationNotification extends Notification {
	private final Account multisigAccount;
	private final Collection<MultisigModification> modifications;

	/**
	 * Creates a new cosignatory modification notification.
	 *
	 * @param multisigAccount The multisig account.
	 * @param modifications The list of modifications.
	 */
	public MultisigModificationNotification(final Account multisigAccount, final Collection<MultisigModification> modifications) {
		super(NotificationType.CosignatoryModification);
		this.multisigAccount = multisigAccount;
		this.modifications = modifications;
	}

	/**
	 * Gets the multisig account.
	 *
	 * @return The multisig account.
	 */
	public Account getMultisigAccount() {
		return this.multisigAccount;
	}

	/**
	 * Gets the list of modifications.
	 *
	 * @return The list of modifications.
	 */
	public Collection<MultisigModification> getModifications() {
		return this.modifications;
	}
}