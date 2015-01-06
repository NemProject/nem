package org.nem.nis.dbmodel;

/**
 * DTO containing a dbTransferTransaction and a dbBlock object.
 */
public class TransferBlockPair {
	private final DbTransferTransaction dbTransferTransaction;
	private final DbBlock dbBlock;

	/**
	 * Creates a dbTransferTransaction dbBlock pair.
	 *
	 * @param dbTransferTransaction The dbTransferTransaction.
	 * @param dbBlock The dbBlock.
	 */
	public TransferBlockPair(final DbTransferTransaction dbTransferTransaction, final DbBlock dbBlock) {
		this.dbTransferTransaction = dbTransferTransaction;
		this.dbBlock = dbBlock;
	}

	/**
	 * Gets the dbTransferTransaction:
	 *
	 * @return The dbTransferTransaction.
	 */
	public DbTransferTransaction getDbTransferTransaction() {
		return this.dbTransferTransaction;
	}

	/**
	 * Gets the dbBlock:
	 *
	 * @return The dbBlock.
	 */
	public DbBlock getDbBlock() {
		return this.dbBlock;
	}
}
