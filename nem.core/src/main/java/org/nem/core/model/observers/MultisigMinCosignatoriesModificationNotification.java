package org.nem.core.model.observers;

import org.nem.core.model.*;

/**
 *  A notification that a minimum cosignatories modification has occurred.
 */
public class MultisigMinCosignatoriesModificationNotification extends Notification {
	private final Account multisigAccount;
	private final MultisigMinCosignatoriesModification modification;

	/**
	 * Creates a new cosignatory modification notification.
	 *
	 * @param multisigAccount The multisig account.
	 * @param modification The modification.
	 */
	public MultisigMinCosignatoriesModificationNotification(final Account multisigAccount, final MultisigMinCosignatoriesModification modification) {
		super(NotificationType.MinCosignatoriesModification);
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
	public MultisigMinCosignatoriesModification getModification() {
		return this.modification;
	}
}
