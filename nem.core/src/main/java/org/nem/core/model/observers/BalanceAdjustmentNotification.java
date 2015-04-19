package org.nem.core.model.observers;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;

/**
 * A notification that one account a unilateral balance adjustment.
 */
public class BalanceAdjustmentNotification extends Notification {
	private final Account account;
	private final Amount amount;

	/**
	 * Creates a new balance adjustment notification.
	 *
	 * @param type The type of adjustment.
	 * @param account The account.
	 * @param amount The amount.
	 */
	public BalanceAdjustmentNotification(final NotificationType type, final Account account, final Amount amount) {
		super(type);
		this.account = account;
		this.amount = amount;
	}

	/**
	 * Gets the account.
	 *
	 * @return The account.
	 */
	public Account getAccount() {
		return this.account;
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