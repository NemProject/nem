package org.nem.nis.poi;

import org.nem.core.model.primitive.*;

/**
 * Information about an account.
 * TODO 20141209 J-*: might want to come up with a better name for this
 */
public class AccountInfo {
	private String label;
	private Amount balance = Amount.ZERO;
	private BlockAmount harvestedBlocks = BlockAmount.ZERO;
	private ReferenceCount refCount = ReferenceCount.ZERO;

	/**
	 * Gets the account's balance.
	 *
	 * @return This account's balance.
	 */
	public Amount getBalance() {
		return this.balance;
	}

	/**
	 * Adds amount to the account's balance.
	 *
	 * @param amount The amount by which to increment the balance.
	 */
	public void incrementBalance(final Amount amount) {
		this.balance = this.balance.add(amount);
	}

	/**
	 * Subtracts amount from the account's balance.
	 *
	 * @param amount The amount by which to decrement the balance.
	 */
	public void decrementBalance(final Amount amount) {
		this.balance = this.balance.subtract(amount);
	}

	/**
	 * Gets number of harvested blocks.
	 *
	 * @return Number of blocks harvested by the account.
	 */
	public BlockAmount getHarvestedBlocks() {
		return this.harvestedBlocks;
	}

	/**
	 * Increments number of blocks harvested by this account by one.
	 */
	public void incrementHarvestedBlocks() {
		this.harvestedBlocks = this.harvestedBlocks.increment();
	}

	/**
	 * Decrements number of blocks harvested by this account by one.
	 */
	public void decrementHarvestedBlocks() {
		this.harvestedBlocks = this.harvestedBlocks.decrement();
	}

	/**
	 * Gets the account's label.
	 *
	 * @return The account's label.
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * Sets the account's label.
	 *
	 * @param label The desired label.
	 */
	public void setLabel(final String label) {
		this.label = label;
	}

	/**
	 * Returns the reference count.
	 *
	 * @return The reference count.
	 */
	public ReferenceCount getReferenceCount() {
		return this.refCount;
	}

	/**
	 * Increments the reference count.
	 *
	 * @return The new value of the reference count.
	 */
	public ReferenceCount incrementReferenceCount() {
		this.refCount = this.refCount.increment();
		return this.refCount;
	}

	/**
	 * Decrements the reference count.
	 *
	 * @return The new value of the reference count.
	 */
	public ReferenceCount decrementReferenceCount() {
		this.refCount = this.refCount.decrement();
		return this.refCount;
	}

	/**
	 * Creates a copy of this info.
	 *
	 * @return A copy of this info.
	 */
	public AccountInfo copy() {
		final AccountInfo copy = new AccountInfo();
		copy.label = this.label;
		copy.balance = this.balance;
		copy.harvestedBlocks = this.harvestedBlocks;
		copy.refCount = this.refCount;
		return copy;
	}
}
