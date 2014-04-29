package org.nem.core.model;

/**
 * 
 */
public class CoinDay {
	private long blockNum;
	
	private Amount amount;

	/**
	 * @return the blockNum
	 */
	public long getBlockNum() {
		return blockNum;
	}

	/**
	 * @param blockNum the blockNum to set
	 */
	public void setBlockNum(long blockNum) {
		this.blockNum = blockNum;
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
	public void setAmount(Amount amount) {
		this.amount = amount;
	}
}
