package org.nem.nis.poi;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.*;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Account information used by poi.
 */
public class PoiAccountInfo {
	private static final long OUTLINK_HISTORY = BlockChainConstants.OUTLINK_HISTORY;
	private final int index;
	private final PoiAccountState accountState;

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

		final AccountImportance importanceInfo = this.accountState.getImportanceInfo();
		final Iterator<AccountLink> outlinks = importanceInfo.getOutlinksIterator(height);

		// weight = out-link amount * DECAY_BASE^(age in days)
		while (outlinks.hasNext()) {
			final AccountLink outlink = outlinks.next();
			final long heightDifference = height.subtract(outlink.getHeight());
			if (OUTLINK_HISTORY < heightDifference && BlockMarkerConstants.BETA_OUTLINK_PRUNING_FORK <= height.getRaw()) {
				continue;
			}

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
	 * The net outlinks can be negative.
	 *
	 * @return The weighted net outlinks.
	 */
	public List<WeightedLink> getNetOutlinks() {
		return this.netOutlinks.entrySet().stream()
				.map(entry -> new WeightedLink(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	/**
	 * Calculates the net out-link score.
	 *
	 * @return The net out-link score.
	 */
	public double getNetOutlinkScore() {
		return this.getNetOutlinks().stream()
				.map(WeightedLink::getWeight)
				.reduce(0.0, Double::sum);
	}
}