package org.nem.core.model.observers;

import org.nem.core.model.Account;

/**
 * A notification that cosignatory modification has occured.
 */
public class CosignatoryModificationNotification extends Notification {
	private final Account multisigAccount;
	private final Account cosignatoryAccount;
	private final int modificationType;

	/**
	 * Creates a new cosignatory modification notification.
	 *
	 * @param multisigAccount The multisig account.
	 * @param cosignatoryAccount The cosigner account.
	 * @param modificationType The type of modification.
	 */
	public CosignatoryModificationNotification(final Account multisigAccount, final Account cosignatoryAccount, final int modificationType) {
		super(NotificationType.CosignatoryModification);
		this.multisigAccount = multisigAccount;
		this.cosignatoryAccount = cosignatoryAccount;
		this.modificationType = modificationType;
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
	 * Gets the cosignatory account.
	 *
	 * @return The cosignatory account.
	 */
	public Account getCosignatoryAccount() {
		return this.cosignatoryAccount;
	}

	/**
	 * Gets the type of signer modification transaction.
	 *
	 * @return The type of signer modification transaction.
	 */
	public int getModificationType() {
		return this.modificationType;
	}
}