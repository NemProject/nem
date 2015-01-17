package org.nem.nis.dbmodel;

/**
 * DTO containing a DbTransferTransaction and a DbBlock object.
 */
public class TransferBlockPair {
	private final AbstractBlockTransfer transfer;
	private final DbBlock dbBlock;

	/**
	 * Creates a pair.
	 *
	 * @param transfer The AbstractBlockTransfer.
	 * @param dbBlock The DbBlock.
	 */
	public TransferBlockPair(final AbstractBlockTransfer transfer, final DbBlock dbBlock) {
		this.transfer = transfer;
		this.dbBlock = dbBlock;
	}

	/**
	 * Gets the DbTransferTransaction:
	 *
	 * @return The DbTransferTransaction.
	 */
	public AbstractBlockTransfer getTransfer() {
		return this.transfer;
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
