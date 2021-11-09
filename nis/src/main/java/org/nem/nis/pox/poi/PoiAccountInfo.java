package org.nem.nis.pox.poi;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Account information used by poi.
 */
public class PoiAccountInfo {
	private final int index;
	private final AccountState accountState;

	private final Map<Address, Double> netOutlinks = new HashMap<>();
	private final List<WeightedLink> outlinks = new ArrayList<>();

	/**
	 * Creates a new POI account info.
	 *
	 * @param index The temporal account index.
	 * @param accountState The account state.
	 * @param height The height at which the strength is evaluated.
	 */
	public PoiAccountInfo(final int index, final AccountState accountState, final BlockHeight height) {
		this(NemGlobals.getBlockChainConfiguration(), index, accountState, height);
	}

	private PoiAccountInfo(final BlockChainConfiguration configuration, final int index, final AccountState accountState,
			final BlockHeight height) {
		this.index = index;
		this.accountState = accountState;

		final int outlinkHistory = configuration.getEstimatedBlocksPerMonth();
		final BlockHeight startHeight = new BlockHeight(Math.max(1, height.getRaw() - outlinkHistory));
		final ReadOnlyAccountImportance importanceInfo = this.accountState.getImportanceInfo();
		final Iterator<AccountLink> outlinks = importanceInfo.getOutlinksIterator(startHeight, height);

		// weight = out-link amount * DECAY_BASE^(age in days)
		while (outlinks.hasNext()) {
			final AccountLink outlink = outlinks.next();
			final long heightDifference = height.subtract(outlink.getHeight());
			final long estimatedBlockPerDay = NemGlobals.getBlockChainConfiguration().getEstimatedBlocksPerDay();
			final long age = heightDifference / estimatedBlockPerDay;
			final double weight = outlink.getAmount().getNumMicroNem() * Math.pow(WeightedBalanceDecayConstants.DECAY_BASE, age);

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
	public AccountState getState() {
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
	 * Gets the weighted net outlinks associated with this account. The net outlinks can be negative.
	 *
	 * @return The weighted net outlinks.
	 */
	public List<WeightedLink> getNetOutlinks() {
		return this.netOutlinks.entrySet().stream().map(entry -> new WeightedLink(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	/**
	 * Calculates the net out-link score.
	 *
	 * @return The net out-link score.
	 */
	public double getNetOutlinkScore() {
		return this.getNetOutlinks().stream().map(WeightedLink::getWeight).reduce(0.0, Double::sum);
	}
}
