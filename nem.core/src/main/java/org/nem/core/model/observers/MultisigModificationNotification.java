package org.nem.core.model.observers;

import org.nem.core.model.*;

/**
 * A notification that cosignatory modification has occured.
 */
public class MultisigModificationNotification extends Notification {
	private final Account multisigAccount;
	private final MultisigCosignatoryModification modification;

	/**
	 * Creates a new cosignatory modification notification.
	 *
	 * @param multisigAccount The multisig account.
	 * @param modification The modification.
	 */
	public MultisigModificationNotification(final Account multisigAccount, final MultisigCosignatoryModification modification) {
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
	public MultisigCosignatoryModification getModification() {
		return this.modification;
	}
}