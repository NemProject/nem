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

	// TODO 20140920 J-G does it make sense to have this separate?
	// TODO 20140922 G-J It certainly does. Even if we won't use it now, it'll be good to have it,
	// when we will want to have apps (i.e. chain explorer) register in nis and receive notifications.
	/**
	 * The notification represents a block harvest reward (this is different from BalanceCredit
	 * because harvest fees can be forwarded whereas regular credits cannot).
	 */
	HarvestReward
}
