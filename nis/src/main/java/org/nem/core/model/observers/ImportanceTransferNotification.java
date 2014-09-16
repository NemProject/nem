package org.nem.core.model.observers;

import org.nem.core.model.Account;

/**
 * A notification that one account has transferred its importance to another account.
 */
public class ImportanceTransferNotification extends Notification {
	private final Account lessor;
	private final Account lessee;

	/**
	 * Creates a new importance transfer notification.
	 *
	 * @param lessor The account leasing its importance.
	 * @param lessee The account borrowing the importance.
	 */
	public ImportanceTransferNotification(final Account lessor, final Account lessee) {
		super(NotificationType.ImportanceTransfer);
		this.lessor = lessor;
		this.lessee = lessee;
	}

	/**
	 * Gets the account leasing its importance.
	 *
	 * @return The account leasing its importance.
	 */
	public Account getLessor() {
		return this.lessor;
	}

	/**
	 * Gets the account borrowing the importance.
	 *
	 * @return The account borrowing the importance.
	 */
	public Account getLessee() {
		return this.lessee;
	}
}