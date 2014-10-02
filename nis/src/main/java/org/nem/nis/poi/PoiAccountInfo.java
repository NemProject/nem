package org.nem.nis.poi;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.nis.secret.*;

import java.util.*;

/**
 * Account information used by poi.
 */
public class PoiAccountInfo {

	public static final Amount MIN_HARVESTING_BALANCE = Amount.fromNem(1000);

	private final int index;
	private final PoiAccountState accountState;
	private final BlockHeight height;

	private final Map<Address, Double> netOutlinks = new HashMap<>();
	private final List<WeightedLink> outlinks = new ArrayList<>();

	/**
	 * Creates a new POI account info.
	 *
	 * @param index The temporal account index.
	 * @param accountState The account state.
	 * @param height The height at which the strength is evaluated.
	 */
	public PoiAccountInfo(final int index, final PoiAccountState accountState, final BlockHeight height) {
		this.index = index;
		this.accountState = accountState;
		this.height = height;

		final AccountImportance importanceInfo = this.accountState.getImportanceInfo();
		final Iterator<AccountLink> outlinks = importanceInfo.getOutlinksIterator(height);

		// weight = out-link amount * DECAY_BASE^(age in days)
		while (outlinks.hasNext()) {
			final AccountLink outlink = outlinks.next();
			final long heightDifference = height.subtract(outlink.getHeight());
			final long age = heightDifference / BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
			final double weight = heightDifference < 0
					? 0.0
					: outlink.getAmount().getNumMicroNem() * Math.pow(WeightedBalanceDecayConstants.DECAY_BASE, age);

			this.outlinks.add(new WeightedLink(outlink.getOtherAccountAddress(), weight));
			this.increment(outlink.getOtherAccountAddress(), weight);
		}
	}

	private void increment(final Address address, final double amount) {
		this.netOutlinks.put(address, this.netOutlinks.getOrDefault(address, 0.0) + amount);
	}

	/**
	 * Gets the account index.
	 *
	 * @return The account index.
	 */
	public int getIndex() {
		return this.index;
	}

	/**
	 * Gets the account state.
	 *
	 * @return The account state.
	 */
	public PoiAccountState getState() {
		return this.accountState;
	}

	/**
	 * Determines whether or not the account is eligible for harvesting.
	 *
	 * @return true if the account is eligible.
	 */
	public boolean canHarvest() {
		return this.accountState.getWeightedBalances().getVested(this.height).compareTo(MIN_HARVESTING_BALANCE) >= 0;
	}

	/**
	 * Adds an inlink to this account info.
	 *
	 * @param inlink The inlink to add.
	 */
	public void addInlink(final WeightedLink inlink) {
		this.increment(inlink.getOtherAccountAddress(), -inlink.getWeight());
	}

	/**
	 * Gets the weighted outlinks associated with this account.
	 *
	 * @return The weighted outlinks.
	 */
	public List<WeightedLink> getOutlinks() {
		return this.outlinks;
	}

	/**
	 * Gets the weighted net outlinks associated with this account.
	 *
	 * @return The weighted net outlinks.
	 */
	public List<WeightedLink> getNetOutlinks() {
		final List<WeightedLink> links = new ArrayList<>();
		for (final Map.Entry<Address, Double> entry : this.netOutlinks.entrySet()) {
			// TODO-CR 20140925 BR: To fix the test PoiContextTest.outlinkScoreVectorIsInitializedCorrectlyWhenThereAreBidirectionalFlows
			// TODO-CR              i commented out the following lines. Does this have negative impact somewhere else?
			/*if (entry.getValue() <= 0) {
				continue;
			}*/

			links.add(new WeightedLink(entry.getKey(), entry.getValue()));
		}

		return links;
	}

	/**
	 * Calculates the net out-link score.
	 *
	 * @return The net out-link score.
	 */
	public double getNetOutlinkScore() {
		final double netOutlinkScore = this.getNetOutlinks().stream()
				.map(WeightedLink::getWeight)
				.reduce(0.0, Double::sum);
		if (netOutlinkScore < 0) {
			// TODO: Idea is to not weight negative outlink scores fully; look into this.
			// TODO-CR 20140925 BR -> M: I adjusted the value for account 2 in the unit test
			// TODO-CR                   PoiContextTest.outlinkScoreVectorIsInitializedCorrectlyWhenThereAreBidirectionalFlows to fix the test.
			// TODO-CR                   Whenever you change this you have to fix that test.
			return netOutlinkScore * 0.2;
		}
		return netOutlinkScore;
	}
}