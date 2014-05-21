package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.*;

import java.util.Iterator;
import java.util.List;

/**
 * Account information used by poi.
 */
public class PoiAccountInfo {

	private static final Amount MIN_FORAGING_BALANCE = Amount.fromNem(1);
	public static final double DECAY_BASE = (double)WeightedBalance.DECAY_NUMERATOR/(double)WeightedBalance.DECAY_DENOMINATOR;

	private final int index;
	private final Account account;
	private final ColumnVector outLinkWeightsVector;

	/**
	 * Creates a new POI account info.
	 *
	 * @param index The temporal account index.
	 * @param account The account.
	 * @param height The height at which the strength is evaluated
	 */
	public PoiAccountInfo(final int index, final Account account, final BlockHeight height) {
		this.index = index;
		this.account = account;

		if (!this.hasOutLinks(height)) {
			this.outLinkWeightsVector = null;
			return;
		}

		final AccountImportance importanceInfo = this.account.getImportanceInfo();
		final Iterator<AccountLink> outLinks = importanceInfo.getOutLinksIterator(height);
		this.outLinkWeightsVector = new ColumnVector(importanceInfo.getOutLinksSize(height));

		// weight = outlink amount * DECAY_BASE^(age in days)
		int i = 0;
		while (outLinks.hasNext()) {
			final AccountLink outLink = outLinks.next();
			long age = (height.getRaw() - outLink.getHeight().getRaw())/BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
			double weight = age < 0? 0.0 : outLink.getAmount().getNumMicroNem() * Math.pow(DECAY_BASE, age);
			this.outLinkWeightsVector.setAt(i, weight);
			++i;
		}
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
		return this.account.getWeightedBalances().getVested(height).compareTo(MIN_FORAGING_BALANCE) >= 0
				&& this.account.getBalance().compareTo(MIN_FORAGING_BALANCE) >= 0;
	}

	/**
	 * Determines if the account has any out-links.
	 *
	 * @return true if the account has any out-links.
	 */
	public boolean hasOutLinks(final BlockHeight blockHeight) {
		return 0 != this.account.getImportanceInfo().getOutLinksSize(blockHeight);
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
	public double getOutLinkScore(final BlockHeight blockHeight) {
		if (!this.hasOutLinks(blockHeight))
			return 0;

		final double weightsMedian = this.outLinkWeightsVector.median();
		return weightsMedian * this.outLinkWeightsVector.size();
	}
}