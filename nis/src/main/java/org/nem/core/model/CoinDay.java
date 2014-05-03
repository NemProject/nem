package org.nem.core.model;

/**
 * 
 */
public class CoinDay {

	private BlockHeight blockHeight;

	private Amount amount;

	/**
	 * Creates CoinDay given height of a block and amount.
	 *
	 * @param blockHeight height of a block
	 * @param amount the amount to set
	 */
	public CoinDay(final BlockHeight blockHeight, final Amount amount) {
		this.blockHeight = blockHeight;
		this.amount = amount;
	}

	/**
	 * @return the blockHeight
	 */
	public BlockHeight getHeight() {
		return blockHeight;
	}

	/**
	 * @return the amount
	 */
	public Amount getAmount() {
		return amount;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(final Amount amount) {
		this.amount = amount;
	}
}
