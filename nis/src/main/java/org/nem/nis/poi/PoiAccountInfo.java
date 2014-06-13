package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;

import java.util.Iterator;

/**
 * Account information used by poi.
 */
public class PoiAccountInfo {

	private static final Amount MIN_FORAGING_BALANCE = Amount.fromNem(1);
	public static final double DECAY_BASE = (double)WeightedBalance.DECAY_NUMERATOR/(double)WeightedBalance.DECAY_DENOMINATOR;

	private final int index;
	private final Account account;
	private final BlockHeight height;
	private final ColumnVector outlinkWeightsVector;

	/**
	 * Creates a new POI account info.
	 *
	 * @param index The temporal account index.
	 * @param account The account.
	 * @param height The height at which the strength is evaluated.
	 */
	public PoiAccountInfo(final int index, final Account account, final BlockHeight height) {
		this.index = index;
		this.account = account;
		this.height = height;

		if (!this.hasOutlinks()) {
			this.outlinkWeightsVector = null;
			return;
		}

		final AccountImportance importanceInfo = this.account.getImportanceInfo();
		final Iterator<AccountLink> outlinks = importanceInfo.getOutlinksIterator(height);
		this.outlinkWeightsVector = new ColumnVector(importanceInfo.getOutlinksSize(height));

		// weight = out-link amount * DECAY_BASE^(age in days)
		int i = 0;
		while (outlinks.hasNext()) {
			final AccountLink outlink = outlinks.next();
			final long heightDifference = height.subtract(outlink.getHeight());
			long age = heightDifference / BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
			double weight = heightDifference < 0 ? 0.0 : outlink.getAmount().getNumMicroNem() * Math.pow(DECAY_BASE, age);
			this.outlinkWeightsVector.setAt(i, weight);
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
	 * Determines whether or not the account is eligible for foraging.
	 *
	 * @return true if the account is eligible.
	 */
	public boolean canForage() {
		return this.account.getWeightedBalances().getVested(this.height).compareTo(MIN_FORAGING_BALANCE) >= 0
				&& this.account.getBalance().compareTo(MIN_FORAGING_BALANCE) >= 0;
	}

	/**
	 * Determines if the account has any out-links.
	 *
	 * @return true if the account has any out-links.
	 */
	public boolean hasOutlinks() {
		return 0 != this.account.getImportanceInfo().getOutlinksSize(this.height);
	}

	/**
	 * Gets the out-links weights vector.
	 *
	 * @return The out-links weight vector.
	 */
	public ColumnVector getOutlinkWeights() {
		return this.outlinkWeightsVector;
	}

	/**
	 * Calculates the out-link score.
	 *
	 * @return The out-link score.
	 */
	public double getOutlinkScore() {
		if (!this.hasOutlinks())
			return 0;

		final double weightsMedian = this.outlinkWeightsVector.median();
		return weightsMedian * this.outlinkWeightsVector.size();
	}
}