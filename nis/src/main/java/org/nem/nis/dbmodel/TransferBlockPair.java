package org.nem.nis.dbmodel;

/**
 * DTO containing an abstract block transfer and a block.
 */
@SuppressWarnings("rawtypes")
public class TransferBlockPair implements Comparable<TransferBlockPair> {
	private final AbstractBlockTransfer transfer;
	private final DbBlock block;

	/**
	 * Creates a pair.
	 *
	 * @param transfer The abstract block transfer.
	 * @param block The block.
	 */
	public TransferBlockPair(final AbstractBlockTransfer transfer, final DbBlock block) {
		this.transfer = transfer;
		this.block = block;
	}

	/**
	 * Gets the abstract block transfer.
	 *
	 * @return The abstract block transfer.
	 */
	public AbstractBlockTransfer getTransfer() {
		return this.transfer;
	}

	/**
	 * Gets the block.
	 *
	 * @return The block.
	 */
	public DbBlock getDbBlock() {
		return this.block;
	}

	@Override
	public int compareTo(final TransferBlockPair o) {
		return -this.getTransfer().getId().compareTo(o.getTransfer().getId());
	}
}
