package org.nem.core.model;

import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;

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
	public HistoricalBalance(final BlockHeight height, final Amount balance) {
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
	 * Chronological comparison of two historical balances
	 *  
	 * @param rhs the historical balance to compare to.
	 */
	@Override
	public int compareTo(final HistoricalBalance rhs) {
		return this.height.compareTo(rhs.getHeight());
	}
}
