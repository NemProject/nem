package org.nem.core.model;

public class VestedBalance implements Comparable<VestedBalance> {
	private final BlockHeight blockHeight;
	private double unvestedBalance;
	private double vestedBalance;

	public VestedBalance(final BlockHeight blockHeight, final Amount unvestedBalance) {
		this.blockHeight = blockHeight;
		this.unvestedBalance = unvestedBalance.getNumMicroNem();
		this.vestedBalance = 0d;
	}

	private VestedBalance(final BlockHeight blockHeight, final double  unvestedBalance, final double vestedBalance) {
		this.blockHeight = blockHeight;
		this.unvestedBalance = unvestedBalance;
		this.vestedBalance = vestedBalance;
	}

	public VestedBalance next() {
		final double newUv = this.unvestedBalance*126d/128d;
		final double move = this.unvestedBalance - newUv;
		return new VestedBalance(
				new BlockHeight(this.blockHeight.getRaw() + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY),
				newUv,
				this.vestedBalance + move);
	}

	public VestedBalance previous() {
		final double newUv = this.unvestedBalance*128d/126d;
		final double move = newUv - this.unvestedBalance;
		return new VestedBalance(
				new BlockHeight(this.blockHeight.getRaw() - BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY),
				newUv,
				this.vestedBalance - move);
	}

	public void add(final Amount amount) {
		this.unvestedBalance = this.unvestedBalance + amount.getNumMicroNem();
	}

	public void sub(final Amount amount) {
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
	public int compareTo(final VestedBalance o) {
		return this.blockHeight.compareTo(o.blockHeight);
	}
}
