package org.nem.nis.secret;

import org.nem.core.model.primitive.*;

/**
 * Calculates vested and unvested balances at a specified block height.
 */
public class WeightedBalance implements Comparable<WeightedBalance> {
	public static final WeightedBalance ZERO = new WeightedBalance(Amount.ZERO, BlockHeight.ONE, Amount.ZERO, 0, 0);

	private final BlockHeight blockHeight;
	private final long unvestedBalance;
	private final long vestedBalance;
	private final Amount balance;

	// TODO: do why do we need amount? we seem to only be using it as a id, which seems odd
	// TODO: i don't think there's any downside with using balance as an id instead (if we even need it)
	private final Amount amount;

	//region createUnvested / createVested

	/**
	 * Creates a weighted balance that is fully unvested.
	 *
	 * @param height The block height.
	 * @param amount The amount.
	 * @return The new weighted balance.
	 */
	public static WeightedBalance createUnvested(final BlockHeight height, final Amount amount) {
		return new WeightedBalance(amount, height, amount, amount.getNumMicroNem(), 0);
	}

	/**
	 * Creates a weighted balance that is fully vested.
	 *
	 * @param height The block height.
	 * @param amount The amount.
	 * @return The new weighted balance.
	 */
	public static WeightedBalance createVested(final BlockHeight height, final Amount amount) {
		return new WeightedBalance(amount, height, amount, 0, amount.getNumMicroNem());
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

	//endregion

	/**
	 * Creates a new weighted balance.
	 *
	 * @param blockHeight The block height.
	 * @param amount The unvested balance.
	 */

	// TODO: rename to send
	public WeightedBalance createSend(final BlockHeight blockHeight, final Amount amount) {
		final Amount balance = this.balance.subtract(amount);
		final double ratio = (double)this.unvestedBalance / (this.unvestedBalance + this.vestedBalance);
		final long sendUv = (long)(ratio * amount.getNumMicroNem());
		// TODO: this probably can hit negative, which probably won't do us anything good
		final long unvested = this.unvestedBalance - sendUv;
		final long vested = this.vestedBalance - (amount.getNumMicroNem() - sendUv);
		return new WeightedBalance(amount, blockHeight, balance, unvested, vested);
	}

	// TODO: rename to receive
	public WeightedBalance createReceive(final BlockHeight blockHeight, final Amount amount) {
		final Amount balance = this.balance.add(amount);
		final long unvested = this.unvestedBalance + amount.getNumMicroNem();
		return new WeightedBalance(amount, blockHeight, balance, unvested, this.vestedBalance);
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
		final long newUv = this.unvestedBalance * WeightedBalanceDecayConstants.DECAY_NUMERATOR / WeightedBalanceDecayConstants.DECAY_DENOMINATOR;
		final long move = this.unvestedBalance - newUv;

		final long blocksPerDay = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
		final long originalHeight = this.blockHeight.getRaw();
		final BlockHeight height = new BlockHeight(1 + ((originalHeight + blocksPerDay - 1) / blocksPerDay) * blocksPerDay);

		return new WeightedBalance(
				Amount.ZERO,
				height,
				this.balance,
				this.unvestedBalance - move,
				this.vestedBalance + move);
	}

	/**
	 * Gets the block height associated with this balance.
	 *
	 * @return The block height.
	 */
	public BlockHeight getBlockHeight() {
		return this.blockHeight;
	}

	/**
	 * Gets the vested balance.
	 *
	 * @return The vested balance.
	 */
	public Amount getVestedBalance() {
		return Amount.fromMicroNem(this.vestedBalance);
	}

	/**
	 * Gets the unvested balance.
	 *
	 * @return The unvested balance.
	 */
	public Amount getUnvestedBalance() {
		return this.balance.subtract(this.getVestedBalance());
	}

	/**
	 * Gets the total amount.
	 *
	 * @return The total amount.
	 */
	public Amount getAmount() {
		return this.amount;
	}

	/**
	 * Gets total balance
	 *
	 * @return The balance.
	 */
	public Amount getBalance() {
		return this.balance;
	}

	@Override
	public int compareTo(final WeightedBalance o) {
		return this.blockHeight.compareTo(o.blockHeight);
	}
}
