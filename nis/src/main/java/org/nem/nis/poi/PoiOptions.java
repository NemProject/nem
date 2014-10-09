package org.nem.nis.poi;

import org.nem.core.model.primitive.Amount;

/**
 * Options that influence the POI calculation.
 */
public class PoiOptions {
	private final Amount minHarvesterBalance;
	private final Amount minOutlinkWeight;
	private final boolean isClusteringEnabled;

	/**
	 * Creates new options.
	 *
	 * @param minHarvesterBalance The minimum (vested) balance required for a harvester.
	 * @param minOutlinkWeight The minimum outlink weight required for an outlink to be relevant.
	 * @param isClusteringEnabled true if clustering should be enabled.
	 */
	public PoiOptions(
			final Amount minHarvesterBalance,
			final Amount minOutlinkWeight,
			final boolean isClusteringEnabled) {
		this.minHarvesterBalance = minHarvesterBalance;
		this.minOutlinkWeight = minOutlinkWeight;
		this.isClusteringEnabled = isClusteringEnabled;
	}

	/**
	 * Gets the minimum (vested) balance required for a harvester.
	 *
	 * @return The minimum (vested) balance.
	 */
	public Amount getMinHarvesterBalance() {
		return this.minHarvesterBalance;
	}

	/**
	 * Gets the minimum outlink weight required for an outlink to be included in the POI calculation.
	 *
	 * @return The minimum outlink weight.
	 */
	public Amount getMinOutlinkWeight() {
		return this.minOutlinkWeight;
	}

	/**
	 * Gets a value indicating whether or not clustering is enabled.
	 *
	 * @return true if clustering should be enabled.
	 */
	public boolean isClusteringEnabled() {
		return this.isClusteringEnabled;
	}
}
