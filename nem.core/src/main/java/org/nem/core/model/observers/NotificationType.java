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
	 * The notification represents a block was harvested (and includes information about the harvester fees).
	 */
	BlockHarvest,

	/**
	 * The notification represents appearance/disappearance of transaction hashes.
	 */
	TransactionHashes,

	/**
	 * The notification represents a cosigner change.
	 */
	CosignatoryModification,

	/**
	 * The notification represents a minimum cosignatories change.
	 */
	MinCosignatoriesModification,

	/**
	 * The notification represents a namespace provision.
	 */
	ProvisionNamespace,

	/**
	 * The notification represents the creation of a mosaic.
	 */
	MosaicCreation
}
