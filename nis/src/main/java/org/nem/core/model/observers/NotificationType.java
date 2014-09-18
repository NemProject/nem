package org.nem.core.model.observers;

/**
 * Known notification types.
 */
public enum NotificationType {

	/**
	 * The notification represents a transfer of NEM.
	 */
	BalanceTransfer,

	/**
	 * The notification represents a credit of NEM.
	 */
	BalanceCredit,

	/**
	 * The notification represents a debit of NEM.
	 */
	BalanceDebit,

	/**
	 * The notification represents an importance transfer.
	 */
	ImportanceTransfer,

	/**
	 * The notification represents the announcement of a potentially new account.
	 */
	Account
}
