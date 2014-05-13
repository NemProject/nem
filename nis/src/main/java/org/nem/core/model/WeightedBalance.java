package org.nem.core.model;

public class WeightedBalance implements Comparable<WeightedBalance> {
	private final BlockHeight blockHeight;
	private double unvestedBalance;
	private double vestedBalance;

	public WeightedBalance(final BlockHeight blockHeight, final Amount unvestedBalance) {
		this.blockHeight = blockHeight;
		this.unvestedBalance = unvestedBalance.getNumMicroNem();
		this.vestedBalance = 0d;
	}

	private WeightedBalance(final BlockHeight blockHeight, final double unvestedBalance, final double vestedBalance) {
		this.blockHeight = blockHeight;
		this.unvestedBalance = unvestedBalance;
		this.vestedBalance = vestedBalance;
	}

	public WeightedBalance next() {
		final double newUv = this.unvestedBalance*126d/128d;
		final double move = this.unvestedBalance - newUv;
		return new WeightedBalance(
				new BlockHeight(this.blockHeight.getRaw() + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY),
				newUv,
				this.vestedBalance + move);
	}

	public WeightedBalance previous() {
		final double newUv = this.unvestedBalance*128d/126d;
		final double move = newUv - this.unvestedBalance;
		return new WeightedBalance(
				new BlockHeight(this.blockHeight.getRaw() - BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY),
				newUv,
				this.vestedBalance - move);
	}

	public void send(final Amount amount) {
		// I guess we shouldn't care about possible div by zero here,
		// as this func shouldn't be called if Account's balance is 0
		double vested = amount.getNumMicroNem()*this.vestedBalance / (this.vestedBalance + this.unvestedBalance);;
		this.vestedBalance = this.vestedBalance - vested;
		this.unvestedBalance = this.unvestedBalance - (amount.getNumMicroNem() - vested);
	}

	public void undoSend(final Amount amount) {
		double vested = amount.getNumMicroNem()*this.vestedBalance / (this.vestedBalance + this.unvestedBalance);;
		this.vestedBalance = this.vestedBalance + vested;
		this.unvestedBalance = this.unvestedBalance + (amount.getNumMicroNem() - vested);
	}

	public void receive(final Amount amount) {
		this.unvestedBalance = this.unvestedBalance + amount.getNumMicroNem();
	}

	public void undoReceive(final Amount amount) {
		if (amount.getNumMicroNem() > this.unvestedBalance)
			throw new IllegalArgumentException("amount must be non-negative");

		this.unvestedBalance = this.unvestedBalance - amount.getNumMicroNem();
	}

	public BlockHeight getBlockHeight() {
		return blockHeight;
	}

	public Amount getUnvestedBalance() {
		return Amount.fromMicroNem(Math.round(unvestedBalance));
	}

	public Amount getVestedBalance() {
		return Amount.fromMicroNem(Math.round(vestedBalance));
	}

	@Override
	public int compareTo(final WeightedBalance o) {
		return this.blockHeight.compareTo(o.blockHeight);
	}
}
