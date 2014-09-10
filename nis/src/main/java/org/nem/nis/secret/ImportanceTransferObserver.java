package org.nem.nis.secret;

import org.nem.core.model.Account;

// TODO 20140909 J-G: does this need to be in core or can it be moved to secret with most of our other observers?
// I'm not sure why I've placed it in core, probably because TransferObserver was in core.

/**
 * An observer that notifies listeners when importance transfers are made.
 */
public interface ImportanceTransferObserver {

	/**
	 * Importance transfer has been made.
	 *
	 * @param sender The creator of importance transfer (owner).
	 * @param recipient The remote account of importance transfer.
	 * @param mode ImportanceTransferTransactionMode
	 */
	public void notifyTransfer(final Account sender, final Account recipient, final int mode);
}
