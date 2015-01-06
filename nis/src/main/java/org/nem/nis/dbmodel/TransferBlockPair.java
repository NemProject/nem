package org.nem.nis.dbmodel;

/**
 * DTO containing a dbTransferTransaction and a db block object.
 */
public class TransferBlockPair {
	private final DbTransferTransaction dbTransferTransaction;
	private final Block block;

	/**
	 * Creates a dbTransferTransaction block pair.
	 *
	 * @param dbTransferTransaction The dbTransferTransaction.
	 * @param block The db block.
	 */
	public TransferBlockPair(final DbTransferTransaction dbTransferTransaction, final Block block) {
		this.dbTransferTransaction = dbTransferTransaction;
		this.block = block;
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
	 * Gets the db block:
	 *
	 * @return The block.
	 */
	public Block getBlock() {
		return this.block;
	}
}
