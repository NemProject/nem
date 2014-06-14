package org.nem.core.model;

import org.nem.core.model.primitive.*;

/**
 * An observer that notifies listeners when transfers are made.
 */
public interface BlockTransferObserver {

	/**
	 * The account has sent the specified amount.
	 *
	 * @param height The block height.
	 * @param account The account.
	 * @param amount The amount.
	 */
	public void notifySend(final BlockHeight height, final Account account, final Amount amount);

	/**
	 * The account has received the specified amount.
	 *
	 * @param height The block height.
	 * @param account The account.
	 * @param amount The amount.
	 */
	public void notifyReceive(final BlockHeight height, final Account account, final Amount amount);

	/**
	 * The account has sent the specified amount but the send is being undone.
	 *
	 * @param height The block height.
	 * @param account The account.
	 * @param amount The amount.
	 */
	public void notifySendUndo(final BlockHeight height, final Account account, final Amount amount);

	/**
	 * The account has received the specified amount but the receive is being undone.
	 *
	 * @param height The block height.
	 * @param account The account.
	 * @param amount The amount.
	 */
	public void notifyReceiveUndo(final BlockHeight height, final Account account, final Amount amount);
}