package org.nem.nis.state;

import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;

/**
 * Represents balances that are always vested.
 */
public class VestedBalances implements VestingBalances {
	private Amount balance;

	public VestedBalances() {
		this.balance = Amount.ZERO;
	}

	public VestedBalances(final Amount amount) {
		this.balance = amount;
	}

	// region ReadOnlyVestingBalances

	@Override
	public Amount getVested(BlockHeight height) {
		return this.balance;
	}

	@Override
	public Amount getUnvested(BlockHeight height) {
		return Amount.ZERO;
	}

	@Override
	public int size() {
		return 1;
	}

	// endregion

	// region VestingBalances

	@Override
	public VestingBalances copy() {
		return new VestedBalances(this.balance);
	}

	@Override
	public void addFullyVested(BlockHeight height, Amount amount) {
		this.balance = this.balance.add(amount);
	}

	@Override
	public void addReceive(BlockHeight height, Amount amount) {
		this.balance = this.balance.add(amount);
	}

	@Override
	public void undoReceive(BlockHeight height, Amount amount) {
		this.balance = this.balance.subtract(amount);
	}

	@Override
	public void addSend(BlockHeight height, Amount amount) {
		this.balance = this.balance.subtract(amount);
	}

	@Override
	public void undoSend(BlockHeight height, Amount amount) {
		this.balance = this.balance.add(amount);
	}

	@Override
	public void convertToFullyVested() {
	}

	@Override
	public void undoChain(BlockHeight height) {
	}

	@Override
	public void prune(BlockHeight minHeight) {
	}

	// endregion
}
