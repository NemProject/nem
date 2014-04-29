package org.nem.core.model;

/**
 * 
 */
public class CoinDay {
	public static final int MAX_BLOCKS_CONSIDERED = 144000; //100 days

	private long blockHeight;

	private Amount amount;

	/**
	 * @return the blockHeight
	 */
	public long getBlockHeight() {
		return blockHeight;
	}

	/**
	 * @param blockHeight
	 *            the blockHeight to set
	 */
	public void setBlockHeight(long blockHeight) {
		this.blockHeight = blockHeight;
	}

	/**
	 * @return the amount
	 */
	public Amount getAmount() {
		return amount;
	}

	/**
	 * @param amount
	 *            the amount to set
	 */
	public void setAmount(Amount amount) {
		this.amount = amount;
	}
}
