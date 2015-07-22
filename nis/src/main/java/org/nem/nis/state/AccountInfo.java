package org.nem.nis.state;

import org.nem.core.model.primitive.*;

/**
 * Information about an account.
 */
public class AccountInfo implements ReadOnlyAccountInfo {
	private String label;
	private Amount balance = Amount.ZERO;
	private BlockAmount harvestedBlocks = BlockAmount.ZERO;
	private ReferenceCount refCount = ReferenceCount.ZERO;

	//region balance

	@Override
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

	//endregion

	//region harvested blocks

	@Override
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

	//endregion

	//region label

	@Override
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

	//endregion

	//region reference count

	@Override
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

	//endregion

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
