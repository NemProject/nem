package org.nem.nis.dbmodel;

/**
 * DTO containing a db transfer and a db block object.
 */
public class TransferBlockPair {

	private final Transfer transfer;
	private final Block block;

	/**
	 * Creates a transfer block pair.
	 *
	 * @param transfer The db transfer.
	 * @param block The db block.
	 */
	public TransferBlockPair(final Transfer transfer, final Block block) {
		this.transfer = transfer;
		this.block = block;
	}

	/**
	 * Gets the db transfer:
	 *
	 * @return The transfer.
	 */
	public Transfer getTransfer() {
		return this.transfer;
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
