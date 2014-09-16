package org.nem.nis.secret;

import org.nem.core.model.Account;

/**
 * An observer that notifies listeners when importance transfers are made.
 */
public interface ImportanceTransferObserver {

	/**
	 * Importance transfer has been made.
	 *
	 * @param sender The creator of importance transfer (owner).
	 * @param recipient The remote account of importance transfer.
	 * @param mode The transaction mode.
	 */
	public void notifyTransfer(final Account sender, final Account recipient, final int mode);
}
