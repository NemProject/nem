package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.*;

import java.util.List;

/**
 * Account information used by poi.
 */
public class PoiAccountInfo {

	private static final Amount MIN_FORAGING_BALANCE = Amount.fromNem(1);

	private final int index;
	private final Account account;
	private final ColumnVector outLinkWeightsVector;
	private final double weightsSum;

	/**
	 * Creates a new POI account info.
	 *
	 * @param index The temporal account index.
	 * @param account The account.
	 */
	public PoiAccountInfo(final int index, final Account account) {
		this.index = index;
		this.account = account;

		if (!this.hasOutLinks()) {
			this.outLinkWeightsVector = null;
			this.weightsSum = 0;
			return;
		}

		final List<AccountLink> outLinks = this.account.getOutlinks();
		this.outLinkWeightsVector = new ColumnVector(outLinks.size());

		double weightsSum = 0;
		for (int i = 0; i < outLinks.size(); ++i) {
			double strength = outLinks.get(i).getStrength();
			this.outLinkWeightsVector.setAt(i, strength);
			weightsSum += strength;
		}

		//this.outLinkWeightsVector.normalize();
		this.weightsSum = weightsSum;
	}

	/**
	 * Gets the account index.
	 *
	 * @return The account index.
	 */
	public int getIndex() { return this.index; }

	/**
	 * Gets the account.
	 *
	 * @return The account.
	 */
	public Account getAccount() { return this.account; }

	/**
	 * Determines whether or not the account is eligible for foraging at the specified block height.
	 *
	 * @param height The block height.
	 * @return true if the account is eligible.
	 */
	public boolean canForage(final BlockHeight height) {
		return this.account.getCoinDayWeightedBalance(height).compareTo(MIN_FORAGING_BALANCE) >= 0
				&& this.account.getBalance().compareTo(MIN_FORAGING_BALANCE) >= 0;
	}

	/**
	 * Determines if the account has any out-links.
	 *
	 * @return true if the account has any out-links.
	 */
	public boolean hasOutLinks() {
		final List<?> outLinks = this.account.getOutlinks();
		return null != outLinks && !outLinks.isEmpty();
	}

	/**
	 * Gets the out-links weights vector.
	 *
	 * @return The out-links weight vector.
	 */
	public ColumnVector getOutLinkWeights() {
		return this.outLinkWeightsVector;
	}

	/**
	 * Calculates the out-link score.
	 *
	 * @return The out-link score.
	 */
	public double getOutLinkScore() {
		if (!this.hasOutLinks())
			return 0;

		final double weightsMedian = this.outLinkWeightsVector.median();
		return weightsMedian * this.outLinkWeightsVector.size() * this.weightsSum;
	}
}