package org.nem.core.model.observers;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;

/**
 * A notification that one account has transferred part of its balance to another account.
 */
public class BalanceTransferNotification extends Notification {
	private final Account sender;
	private final Account recipient;
	private final Amount amount;

	/**
	 * Creates a new balance transfer notification.
	 *
	 * @param sender The sender.
	 * @param recipient The recipient.
	 * @param amount The amount.
	 */
	public BalanceTransferNotification(final Account sender, final Account recipient, final Amount amount) {
		super(NotificationType.BalanceTransfer);
		this.sender = sender;
		this.recipient = recipient;
		this.amount = amount;
	}

	/**
	 * Gets the sender.
	 *
	 * @return The sender.
	 */
	public Account getSender() {
		return this.sender;
	}

	/**
	 * Gets the recipient.
	 *
	 * @return The recipient.
	 */
	public Account getRecipient() {
		return this.recipient;
	}

	/**
	 * Gets the amount.
	 *
	 * @return The amount.
	 */
	public Amount getAmount() {
		return this.amount;
	}
}