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
	Account,

	/**
	 * The notification represents a block harvest reward (this is different from BalanceCredit
	 * because harvest fees can be forwarded whereas regular credits cannot).
	 */
	HarvestReward,

	/**
	 * The notification represents appearance/disappearance of transaction hashes.
	 */
	TransactionHashes
}
