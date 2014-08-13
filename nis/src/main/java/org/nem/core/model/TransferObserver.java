package org.nem.core.model;

import org.nem.core.model.primitive.Amount;

/**
 * An observer that notifies listeners when transfers are made.
 */
public interface TransferObserver {

	/**
	 * The sender has transferred the specified amount to the recipient.
	 *
	 * @param sender The sender.
	 * @param recipient The recipient.
	 * @param amount The amount.
	 */
	public void notifyTransfer(final Account sender, final Account recipient, final Amount amount);

	/**
	 * The account has been credited the specified amount.
	 *
	 * @param account The account.
	 * @param amount The amount.
	 */
	public void notifyCredit(final Account account, final Amount amount);

	/**
	 * The account has been debited the specified amount.
	 *
	 * @param account The account.
	 * @param amount The amount.
	 */
	public void notifyDebit(final Account account, final Amount amount);
}