package org.nem.core.model;

/**
 * An observer that notifies listeners when transfers are made.
 */
public interface BlockTransferObserver {

	/**
	 * The sender has transferred the specified amount to the recipient.
	 *
	 * @param height The block height.
	 * @param sender The sender.
	 * @param recipient The recipient.
	 * @param amount The amount.
	 */
	public void notifyTransfer(final BlockHeight height, final Account sender, final Account recipient, final Amount amount) ;

	/**
	 * The account has been credited the specified amount.
	 *
	 * @param height The block height.
	 * @param account The account.
	 * @param amount The amount.
	 */
	public void notifyCredit(final BlockHeight height, final Account account, final Amount amount);

	/**
	 * The account has been debited the specified amount.
	 *
	 * @param height The block height.
	 * @param account The account.
	 * @param amount The amount.
	 */
	public void notifyDebit(final BlockHeight height, final Account account, final Amount amount);
}