package org.nem.core.model.observers;

import org.nem.core.model.Account;

/**
 * A notification that represents the announcement of a potentially new account.
 */
public class AccountNotification extends Notification {
	private final Account account;

	/**
	 * Creates a new account notification.
	 *
	 * @param account The account.
	 */
	public AccountNotification(final Account account) {
		super(NotificationType.Account);
		this.account = account;
	}

	/**
	 * Gets the potentially new account.
	 *
	 * @return The potentially new account.
	 */
	public Account getAccount() {
		return this.account;
	}
}
