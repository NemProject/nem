package org.nem.core.model.observers;

import org.nem.core.model.*;

/**
 * A notification that one account has transferred its importance to another account.
 */
public class ImportanceTransferNotification extends Notification {
	private final Account lessor;
	private final Account lessee;
	private final ImportanceTransferMode mode;

	/**
	 * Creates a new importance transfer notification.
	 *
	 * @param lessor The account leasing its importance.
	 * @param lessee The account borrowing the importance.
	 * @param mode The mode of importance transfer transaction.
	 */
	public ImportanceTransferNotification(final Account lessor, final Account lessee, final ImportanceTransferMode mode) {
		super(NotificationType.ImportanceTransfer);
		this.lessor = lessor;
		this.lessee = lessee;
		this.mode = mode;
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

	/**
	 * Gets the mode of importance transfer.
	 *
	 * @return The mode of importance transfer.
	 */
	public ImportanceTransferMode getMode() {
		return this.mode;
	}
}