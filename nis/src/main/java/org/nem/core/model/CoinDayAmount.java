package org.nem.core.model;

/**
 * Class representing a coinday amount.
 */
public class CoinDayAmount {

	private Amount unweightedAmount = Amount.ZERO;

	private Amount weightedAmount = Amount.ZERO;

	/**
	 * Default constructor.
	 */
	public CoinDayAmount() {
	}

	/**
	 * @param unweightedAmount
	 * @param weightedAmount
	 */
	public CoinDayAmount(Amount unweightedAmount, Amount weightedAmount) {
		this.unweightedAmount = unweightedAmount;
		this.weightedAmount = weightedAmount;
	}

	public void addUnWeightedAmount(final Amount amt) {
		unweightedAmount.add(amt);
	}

	public void addWeightedAmount(final Amount amt) {
		weightedAmount.add(amt);
	}
	
	public void subtractUnweightedAmount(final Amount amt) {
		unweightedAmount.subtract(amt);
	}

	/**
	 * @return the unweightedAmount
	 */
	public Amount getUnweightedAmount() {
		return unweightedAmount;
	}

	/**
	 * @param unweightedAmount
	 *            the unweightedAmount to set
	 */
	public void setUnweightedAmount(Amount unweightedAmount) {
		this.unweightedAmount = unweightedAmount;
	}

	/**
	 * @return the weightedAmount
	 */
	public Amount getWeightedAmount() {
		return weightedAmount;
	}

	/**
	 * @param weightedAmount
	 *            the weightedAmount to set
	 */
	public void setWeightedAmount(Amount weightedAmount) {
		this.weightedAmount = weightedAmount;
	}
}
