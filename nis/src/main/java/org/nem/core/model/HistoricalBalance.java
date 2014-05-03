package org.nem.core.model;

/**
 * Represents a balance of an account at a certain block height.
 */
public class HistoricalBalance implements Comparable<HistoricalBalance> {

	private final BlockHeight height;
	private Amount balance = Amount.ZERO;

	/**
	 * Creates a historical balance.
	 *
	 * @param height the block height.
	 * @param balance the balance at that height
	 */
	public HistoricalBalance(final BlockHeight blockHeight, final Amount balance) {
		this.height = blockHeight;
		this.balance = balance;
	}

	/**
	 * Creates a historical balance.
	 *
	 * @param height the block height as long.
	 * @param amount the amount at that height as long
	 */
	public HistoricalBalance(final long height, final long amount) {
		this.height = new BlockHeight(height);
		this.balance = Amount.fromMicroNem(amount);
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
	 * Chronological comparison of two historical balances
	 *  
	 * @param rhs the historical balance to compare to.
	 */
	@Override
	public int compareTo(final HistoricalBalance rhs) {
		if (this.height.getRaw() < rhs.height.getRaw()) {
			return -1;
		} 
		else if (this.height.getRaw() > rhs.height.getRaw()) {
			return 1;
		}
		return 0;
	}
}
