package org.nem.core.model;

/**
 *
 */
public class CoinDay implements Comparable<CoinDay> {
	private final BlockHeight height;
	private Amount balance = Amount.ZERO;

	/**
	 * Creates a coinday.
	 *
	 * @param height the block height.
	 * @param balance the balance at that height
	 */
	public CoinDay(final BlockHeight height, final Amount balance) {
		if (balance == null) {
			throw new IllegalArgumentException("CoinDay balance is null");
		}
		this.height = height;
		this.balance = balance;
	}

	/**
	 * Returns the block height
	 * 
	 * @return the block height object
	 */
	public BlockHeight getHeight() {
		return height;
	}
	
	/**
	 * Returns the balance
	 * 
	 * @return the Amount object
	 */
	public Amount getBalance() {
		return balance;
	}
	
	/**
	 * Increase the balance by a given amount
	 * 
	 * @param amount the amount to add to the balance
	 */
	public void add(final Amount amount) {
		this.balance = this.balance.add(amount);
	}
	
	/**
	 * Decrease the balance by a given amount
	 * 
	 * @param amount the amount to subtract to the balance
	 */
	public void subtract(final Amount amount) {
		this.balance = this.balance.subtract(amount);
	}
	
	/**
	 * Chronological comparison of two coindays
	 *  
	 * @param rhs the coinday to compare to.
	 */
	@Override
	public int compareTo(final CoinDay rhs) {
		return this.height.compareTo(rhs.getHeight());
	}
}
