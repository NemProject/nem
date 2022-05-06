package org.nem.nis.state;

import org.nem.core.model.NemGlobals;
import org.nem.core.model.primitive.*;

/**
 * Calculates vested and unvested balances at a specified block height. <br>
 * This class is really just an implementation detail of WeightedBalances (it is package private). As a result, it holds a few things that
 * only make sense the context of that class (like amount).
 */
class WeightedBalance implements Comparable<WeightedBalance> {
	public static final WeightedBalance ZERO = new WeightedBalance(Amount.ZERO, BlockHeight.ONE, Amount.ZERO, 0, 0);

	private final BlockHeight blockHeight;
	private final long unvestedBalance;
	private final long vestedBalance;
	private final Amount balance;
	private final Amount amount;

	// region create*

	/**
	 * Creates a weighted balance that is fully unvested.
	 *
	 * @param height The block height.
	 * @param amount The amount.
	 * @return The new weighted balance.
	 */
	public static WeightedBalance createUnvested(final BlockHeight height, final Amount amount) {
		return create(height, Amount.ZERO, amount);
	}

	/**
	 * Creates a weighted balance that is fully vested.
	 *
	 * @param height The block height.
	 * @param amount The amount.
	 * @return The new weighted balance.
	 */
	public static WeightedBalance createVested(final BlockHeight height, final Amount amount) {
		return create(height, amount, Amount.ZERO);
	}

	/**
	 * Creates a weighed balance that is partially vested and unvested.
	 *
	 * @param height The block height.
	 * @param vested The vested amount.
	 * @param unvested The unvested amount.
	 * @return The new weighted balance.
	 */
	public static WeightedBalance create(final BlockHeight height, final Amount vested, final Amount unvested) {
		final Amount balance = vested.add(unvested);
		return new WeightedBalance(balance, height, balance, unvested.getNumMicroNem(), vested.getNumMicroNem());
	}

	private WeightedBalance(final Amount amount, final BlockHeight blockHeight, final Amount balance, final long unvestedBalance,
			final long vestedBalance) {
		this.amount = amount;
		this.blockHeight = blockHeight;
		this.unvestedBalance = unvestedBalance;
		this.vestedBalance = vestedBalance;
		this.balance = balance;
	}

	// endregion

	/**
	 * Creates a new weighted balance representing a new relative outflow (send). This decreases the balance.
	 *
	 * @param blockHeight The block height.
	 * @param amount The amount sent.
	 * @return The new weighted balance.
	 */
	public WeightedBalance createSend(final BlockHeight blockHeight, final Amount amount) {
		final Amount balance = this.balance.subtract(amount);
		final double ratio = (double) this.unvestedBalance / (this.unvestedBalance + this.vestedBalance);
		final long sendUv = (long) (ratio * amount.getNumMicroNem());
		long unvested = this.unvestedBalance - sendUv;
		long vested = this.vestedBalance - (amount.getNumMicroNem() - sendUv);
		if (0 > vested) {
			// must be a rounding error: too much vested is send.
			unvested += vested;
			vested = 0;
		}
		return new WeightedBalance(amount, blockHeight, balance, unvested, vested);
	}

	/**
	 * Creates a new weighted balance representing a new relative inflow (receive). This increases the balance.
	 *
	 * @param blockHeight The block height.
	 * @param amount The amount received.
	 * @return The new weighted balance.
	 */
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
		final long newUv = this.unvestedBalance * WeightedBalanceDecayConstants.DECAY_NUMERATOR
				/ WeightedBalanceDecayConstants.DECAY_DENOMINATOR;
		final long move = this.unvestedBalance - newUv;

		final long blocksPerDay = NemGlobals.getBlockChainConfiguration().getEstimatedBlocksPerDay();
		final long originalHeight = this.blockHeight.getRaw();
		final BlockHeight height = new BlockHeight(1 + ((originalHeight + blocksPerDay - 1) / blocksPerDay) * blocksPerDay);

		return new WeightedBalance(Amount.ZERO, height, this.balance, this.unvestedBalance - move, this.vestedBalance + move);
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
	 * Gets total balance. This is always the sum of the vested and unvested balance.
	 *
	 * @return The balance.
	 */
	public Amount getBalance() {
		return this.balance;
	}

	/**
	 * Gets the amount of the inflow or outflow that is associated with this balance. This is typically one of: - The magnitude of the
	 * initial balance if this weighted balance was created by a factory function. - The magnitude of the inflow or outflow if this weighted
	 * balance was created by a send or receive. - Amount.ZERO if this weighted balance was created by aging.
	 *
	 * @return The total amount.
	 */
	public Amount getAmount() {
		return this.amount;
	}

	@Override
	public int compareTo(final WeightedBalance o) {
		return this.blockHeight.compareTo(o.blockHeight);
	}
}
