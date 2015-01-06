package org.nem.core.model.observers;

import org.nem.core.model.*;

/**
 * A notification that cosignatory modification has occured.
 */
public class MultisigModificationNotification extends Notification {
	private final Account multisigAccount;
	private final MultisigModification modification;

	/**
	 * Creates a new cosignatory modification notification.
	 *
	 * @param multisigAccount The multisig account.
	 * @param modification The modification.
	 */
	public MultisigModificationNotification(final Account multisigAccount, final MultisigModification modification) {
		super(NotificationType.CosignatoryModification);
		this.multisigAccount = multisigAccount;
		this.modification = modification;
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
	 * Gets the modification.
	 *
	 * @return The modification.
	 */
	public MultisigModification getModification() {
		return this.modification;
	}
}