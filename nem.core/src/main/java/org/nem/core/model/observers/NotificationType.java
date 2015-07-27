package org.nem.core.model.observers;

/**
 * Known notification types.
 */
public enum NotificationType {

	/**
	 * The notification represents a transfer of XEM.
	 */
	BalanceTransfer,

	/**
	 * The notification represents a credit of XEM.
	 */
	BalanceCredit,

	/**
	 * The notification represents a debit of XEM.
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
	 * The notification represents the creation of a mosaic definition.
	 */
	MosaicDefinitionCreation,

	/**
	 * The notification represents a supply change for a smart tile type.
	 */
	SmartTileSupplyChange,

	/**
	 * The notification represents a transfer of a smart tile.
	 */
	SmartTileTransfer
}
