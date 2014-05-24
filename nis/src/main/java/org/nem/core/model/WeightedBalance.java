package org.nem.core.model;

import java.math.BigInteger;

/**
 * Calculates vested and unvested balances at a specified block height.
 */
public class WeightedBalance implements Comparable<WeightedBalance> {
	public static final long DECAY_NUMERATOR = 9;
	public static final long DECAY_DENOMINATOR = 10;
	
	private final BlockHeight blockHeight;
	private BigInteger unvestedBalance;
	private BigInteger vestedBalance;
	private Amount balance;

	/**
	 * Creates a new weighted balance.
	 *
	 * @param blockHeight The block height.
	 * @param unvestedBalance The unvested balance.
	 */
	public WeightedBalance(final BlockHeight blockHeight, final Amount unvestedBalance) {
		this(blockHeight, unvestedBalance, BigInteger.valueOf(unvestedBalance.getNumMicroNem()), BigInteger.ZERO);
	}

	private WeightedBalance(
			final BlockHeight blockHeight,
			final Amount balance,
			final BigInteger unvestedBalance,
			final BigInteger vestedBalance) {
		this.blockHeight = blockHeight;
		this.unvestedBalance = unvestedBalance;
		this.vestedBalance = vestedBalance;
		this.balance = balance;

		reduce();
	}

	/**
	 * Creates a deep copy of this weighted balance.
	 *
	 * @return A copy of this weighted balance.
	 */
	public WeightedBalance copy() {
		return new WeightedBalance(this.blockHeight, this.balance, this.unvestedBalance, this.vestedBalance);
	}

	private void reduce() {
		final BigInteger gcd = this.vestedBalance.gcd(this.unvestedBalance);
		if (gcd.equals(BigInteger.ZERO)) {
			return;
		}

		// last multiply, in order to always keep vested/unvested balance > balance
		this.vestedBalance = this.vestedBalance.divide(gcd).multiply(BigInteger.valueOf(this.balance.getNumMicroNem()));
		this.unvestedBalance = this.unvestedBalance.divide(gcd).multiply(BigInteger.valueOf(this.balance.getNumMicroNem()));
	}

	/**
	 * Gets the next weighted balance one day in the future.
	 *
	 * @return The next weighted balance.
	 */
	public WeightedBalance next() {
		final BigInteger base = BigInteger.valueOf(DECAY_DENOMINATOR);
		final BigInteger newUv = this.unvestedBalance.multiply(BigInteger.valueOf(DECAY_NUMERATOR));
		final BigInteger move = this.unvestedBalance.multiply(base).subtract(newUv);

		return new WeightedBalance(
				new BlockHeight(this.blockHeight.getRaw() + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY),
				this.balance,
				this.unvestedBalance.multiply(base).subtract(move),
				this.vestedBalance.multiply(base).add(move)
		);
	}

	/**
	 * Gets the previous weighted balance one day in the past.
	 *
	 * @return The previous weighted balance.
	 */
	public WeightedBalance previous() {
		final BigInteger base = BigInteger.valueOf(DECAY_NUMERATOR);
		final BigInteger newUv = this.unvestedBalance.multiply(BigInteger.valueOf(DECAY_DENOMINATOR));
		final BigInteger move = newUv.subtract(this.unvestedBalance.multiply(base));

		return new WeightedBalance(
				new BlockHeight(this.blockHeight.getRaw() - BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY),
				this.balance,
				this.unvestedBalance.multiply(base).add(move),
				this.vestedBalance.multiply(base).subtract(move));
	}

	/**
	 * Notifies this balance that an amount was sent.
	 *
	 * @param amount The amount.
	 */
	public void send(final Amount amount) {
		// no need to do anything, send keeps the ratio
		this.balance = this.balance.subtract(amount);
	}

	/**
	 * Notifies this balance that an amount that was sent was undone.
	 *
	 * @param amount The amount.
	 */
	public void undoSend(final Amount amount) {
		this.balance = this.balance.add(amount);
	}

	/**
	 * Notifies this balance that an amount was received.
	 *
	 * @param amount The amount.
	 */
	public void receive(final Amount amount) {
		this.unvestedBalance = this.unvestedBalance.add(BigInteger.valueOf(amount.getNumMicroNem()));
		this.balance = this.balance.add(amount);
	}

	/**
	 * Notifies this balance that an amount that was received was undone.
	 *
	 * @param amount The amount.
	 */
	public void undoReceive(final Amount amount) {
		if (amount.compareTo(getUnvestedBalance()) > 0)
			throw new IllegalArgumentException("amount must be non-negative");

		this.unvestedBalance = this.unvestedBalance.subtract(BigInteger.valueOf(amount.getNumMicroNem()));
		this.balance = this.balance.subtract(amount);
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
		final BigInteger sum = this.vestedBalance.add(this.unvestedBalance);
		if (sum.compareTo(BigInteger.ZERO) == 0) {
			return Amount.ZERO;
		}
		return Amount.fromMicroNem(
				this.vestedBalance.multiply(BigInteger.valueOf(this.balance.getNumMicroNem())).divide(sum).longValue()
		);
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
}
