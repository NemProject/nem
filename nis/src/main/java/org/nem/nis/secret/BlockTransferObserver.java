package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.observers.NamedObserver;
import org.nem.core.model.primitive.*;

/**
 * An observer that notifies listeners when transfers are made.
 */
public interface BlockTransferObserver extends NamedObserver {

	/**
	 * The account has sent the specified amount.
	 *
	 * @param height The block height.
	 * @param account The account.
	 * @param amount The amount.
	 */
	void notifySend(final BlockHeight height, final Account account, final Amount amount);

	/**
	 * The account has received the specified amount.
	 *
	 * @param height The block height.
	 * @param account The account.
	 * @param amount The amount.
	 */
	void notifyReceive(final BlockHeight height, final Account account, final Amount amount);

	/**
	 * The account has sent the specified amount but the send is being undone.
	 *
	 * @param height The block height.
	 * @param account The account.
	 * @param amount The amount.
	 */
	void notifySendUndo(final BlockHeight height, final Account account, final Amount amount);

	/**
	 * The account has received the specified amount but the receive is being undone.
	 *
	 * @param height The block height.
	 * @param account The account.
	 * @param amount The amount.
	 */
	void notifyReceiveUndo(final BlockHeight height, final Account account, final Amount amount);
}
