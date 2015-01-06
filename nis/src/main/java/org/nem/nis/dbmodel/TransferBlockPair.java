package org.nem.nis.dbmodel;

/**
 * DTO containing a DbTransferTransaction and a DbBlock object.
 */
public class TransferBlockPair {
	private final DbTransferTransaction dbTransferTransaction;
	private final DbBlock dbBlock;

	/**
	 * Creates a pair.
	 *
	 * @param dbTransferTransaction The DbTransferTransaction.
	 * @param dbBlock The DbBlock.
	 */
	public TransferBlockPair(final DbTransferTransaction dbTransferTransaction, final DbBlock dbBlock) {
		this.dbTransferTransaction = dbTransferTransaction;
		this.dbBlock = dbBlock;
	}

	/**
	 * Gets the DbTransferTransaction:
	 *
	 * @return The DbTransferTransaction.
	 */
	public DbTransferTransaction getDbTransferTransaction() {
		return this.dbTransferTransaction;
	}

	/**
	 * Gets the DbBlock:
	 *
	 * @return The DbBlock.
	 */
	public DbBlock getDbBlock() {
		return this.dbBlock;
	}
}
