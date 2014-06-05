package org.nem.core.model;

/**
 * Calculates vested and unvested balances at a specified block height.
 */
public class WeightedBalance implements Comparable<WeightedBalance> {
	public static final long DECAY_NUMERATOR = 98;
	public static final long DECAY_DENOMINATOR = 100;
	public static final WeightedBalance ZERO =  new WeightedBalance(Amount.ZERO, BlockHeight.ONE, Amount.ZERO, 0, 0);

	private final BlockHeight blockHeight;
	private long unvestedBalance;
	private long vestedBalance;
	private Amount balance;
	private Amount amount;

	/**
	 * Creates a new weighted balance.
	 *
	 * @param blockHeight The block height.
	 * @param amount The unvested balance.
	 */

	public WeightedBalance createSend(final BlockHeight blockHeight, final Amount amount) {
		final Amount balance = this.balance.subtract(amount);
		final double ratio = (double)this.unvestedBalance / (this.unvestedBalance + this.vestedBalance);
		final long sendUv = (long)(ratio * amount.getNumMicroNem());
		// TODO: this probably can hit negative, which probably won't do us anything good
		final long unvested = this.unvestedBalance - sendUv;
		final long vested = this.vestedBalance - (amount.getNumMicroNem() - sendUv);
		return new WeightedBalance(amount, blockHeight, balance, unvested, vested);
	}

	public WeightedBalance createReceive(final BlockHeight blockHeight, final Amount amount) {
		final Amount balance = this.balance.add(amount);
		final long unvested = this.unvestedBalance + amount.getNumMicroNem();
		return new WeightedBalance(amount, blockHeight, balance, unvested, this.vestedBalance);
	}

	private WeightedBalance(
			final Amount amount,
			final BlockHeight blockHeight,
			final Amount balance,
			final long unvestedBalance,
			final long vestedBalance) {
		this.amount = amount;
		this.blockHeight = blockHeight;
		this.unvestedBalance = unvestedBalance;
		this.vestedBalance = vestedBalance;
		this.balance = balance;
	}

	/**
	 * Creates a deep copy of this weighted balance.
	 *
	 * @return A copy of this weighted balance.
	 */
	public WeightedBalance copy() {
		return new WeightedBalance(this.amount, this.blockHeight, this.balance, this.unvestedBalance, this.vestedBalance);
	}

	/**
	 * Gets the next weighted balance one day in the future.
	 *
	 * @return The next weighted balance.
	 */
	public WeightedBalance next() {
		final long newUv = this.unvestedBalance * DECAY_NUMERATOR / DECAY_DENOMINATOR;
		final long move = this.unvestedBalance - newUv;

		final long h = this.blockHeight.getRaw();
		final BlockHeight height = new BlockHeight(1 + ((h + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY - 1)/BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY)* BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY);

		return new WeightedBalance(
				Amount.ZERO,
				height,
				this.balance,
				this.unvestedBalance - move,
				this.vestedBalance + move
		);
	}

	/**
	 * Gets the block height associated with this balance.
	 *
	 * @return The block height.
	 */
	public BlockHeight getBlockHeight() {
		return blockHeight;
	}

	/**
	 * Gets the vested balance.
	 *
	 * @return The vested balance.
	 */
	public Amount getVestedBalance() {
		if (this.balance.equals(Amount.ZERO) || (this.vestedBalance + this.unvestedBalance) == 0) {
			return Amount.ZERO;
		}

		return Amount.fromMicroNem(this.vestedBalance);
	}

	/**
	 * Gets the unvested balance.
	 *
	 * @return The unvested balance.
	 */
	public Amount getUnvestedBalance() {
		return this.balance.subtract(getVestedBalance());
	}

	@Override
	public int compareTo(final WeightedBalance o) {
		return this.blockHeight.compareTo(o.blockHeight);
	}

	public Amount getAmount() {
		return amount;
	}
}
