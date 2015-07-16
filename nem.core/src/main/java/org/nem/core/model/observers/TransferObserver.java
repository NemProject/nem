package org.nem.core.model.observers;

import org.nem.core.model.Account;
import org.nem.core.model.mosaic.SmartTile;
import org.nem.core.model.primitive.*;

/**
 * An observer that notifies listeners when balance transfers are made.
 */
public interface TransferObserver extends NamedObserver {

	/**
	 * The sender has transferred the specified amount of XEM to the recipient.
	 *
	 * @param sender The sender.
	 * @param recipient The recipient.
	 * @param amount The amount.
	 */
	void notifyTransfer(final Account sender, final Account recipient, final Amount amount);

	/**
	 * The sender has transferred the specified quantity of a smart tile to the recipient.
	 * TODO 20150715 J-B: so, i think SmartTile should be mosaic id
	 * TODO 20150715 J-B: i might also have a "special" MosaicId for XEM and remove the other notifyTransfer overload but not sure about this
	 *
	 * @param sender The sender.
	 * @param recipient The recipient.
	 * @param quantity The quantity.
	 * @param smartTile The smart tile.
	 */
	void notifyTransfer(final Account sender, final Account recipient, final Quantity quantity, final SmartTile smartTile);

	/**
	 * The account has been credited the specified amount.
	 *
	 * @param account The account.
	 * @param amount The amount.
	 */
	void notifyCredit(final Account account, final Amount amount);

	/**
	 * The account has been debited the specified amount.
	 *
	 * @param account The account.
	 * @param amount The amount.
	 */
	void notifyDebit(final Account account, final Amount amount);
}