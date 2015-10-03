package org.nem.nis.state;

import org.nem.core.model.primitive.*;

/**
 * Represents balances that are always vested.
 */
public class AlwaysVestedBalances implements WeightedBalances {
	private Amount balance;

	/**
	 * Creates always vested balances with an initial amount of zero.
	 */
	public AlwaysVestedBalances() {
		this.balance = Amount.ZERO;
	}

	/**
	 * Creates always vested balances with an initial amount.
	 *
	 * @param amount The initial amount.
	 */
	public AlwaysVestedBalances(final Amount amount) {
		this.balance = amount;
	}

	// region ReadOnlyWeightedBalances

	@Override
	public Amount getVested(final BlockHeight height) {
		return this.balance;
	}

	@Override
	public Amount getUnvested(final BlockHeight height) {
		return Amount.ZERO;
	}

	@Override
	public int size() {
		return 1;
	}

	// endregion

	// region WeightedBalances

	@Override
	public AlwaysVestedBalances copy() {
		return new AlwaysVestedBalances(this.balance);
	}

	@Override
	public void addFullyVested(final BlockHeight height, final Amount amount) {
		this.addReceive(height, amount);
	}

	@Override
	public void addReceive(final BlockHeight height, final Amount amount) {
		this.balance = this.balance.add(amount);
	}

	@Override
	public void undoReceive(final BlockHeight height, final Amount amount) {
		this.balance = this.balance.subtract(amount);
	}

	@Override
	public void addSend(final BlockHeight height, final Amount amount) {
		this.balance = this.balance.subtract(amount);
	}

	@Override
	public void undoSend(final BlockHeight height, final Amount amount) {
		this.balance = this.balance.add(amount);
	}

	@Override
	public void convertToFullyVested() {
	}

	@Override
	public void undoChain(final BlockHeight height) {
	}

	@Override
	public void prune(final BlockHeight minHeight) {
	}

	// endregion
}
