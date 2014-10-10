package org.nem.nis.poi;

import org.nem.core.model.primitive.Amount;

/**
 * A builder for creating poi options.
 */
public class PoiOptionsBuilder {
	private Amount minHarvesterBalance = Amount.fromNem(1000);
	private Amount minOutlinkWeight = Amount.ZERO;
	private boolean isClusteringEnabled = true;
	private double teleportationProbability = .75; // For NCDawareRank
	private double interLevelTeleportationProbability = .1; // For NCDawareRank

	/**
	 * Sets the minimum (vested) balance required for a harvester.
	 *
	 * @param minHarvesterBalance The minimum (vested) balance.
	 */
	public void setMinHarvesterBalance(final Amount minHarvesterBalance) {
		this.minHarvesterBalance = minHarvesterBalance;
	}

	/**
	 * Sets the minimum outlink weight required for an outlink to be included in the POI calculation.
	 *
	 * @param minOutlinkWeight The minimum outlink weight.
	 */
	public void setMinOutlinkWeight(final Amount minOutlinkWeight) {
		this.minOutlinkWeight = minOutlinkWeight;
	}

	/**
	 * Sets a value indicating whether or not clustering should be enabled.
	 *
	 * @param isClusteringEnabled true if clustering should be enabled.
	 */
	public void setIsClusteringEnabled(final boolean isClusteringEnabled) {
		this.isClusteringEnabled = isClusteringEnabled;
	}

	/**
	 * Sets the teleportation probability.
	 *
	 * @param probability The teleportation probability.
	 */
	public void setTeleportationProbability(final double probability) {
		this.teleportationProbability = probability;
	}

	/**
	 * Sets the inter-level teleportation probability.
	 *
	 * @param probability The inter-level teleportation probability.
	 */
	public void setInterLevelTeleportationProbability(final double probability) {
		this.interLevelTeleportationProbability = probability;
	}

	//	//clusteringAlgs = ['SingleClusterScan', 'OutlierScan', 'FastScanClusteringStrategy']
//	//negativeOutlinkWeights = [0, 20, 40, 60, 80, 100]
//	//outlierWeights = [0.85, 0.9, 0.95]
//	//mus = [1, 2, 3, 4, 5]
//	//epsilons = [0.15, 0.25, 0.35, 0.45, 0.55, 0.65, 0.75, 0.85, 0.95]

	/**
	 * Creates a new poi options.
	 *
	 * @return The poi options
	 */
	public PoiOptions create() {
		return new PoiOptions() {

			@Override
			public Amount getMinHarvesterBalance() {
				return PoiOptionsBuilder.this.minHarvesterBalance;
			}

			@Override
			public Amount getMinOutlinkWeight() {
				return PoiOptionsBuilder.this.minOutlinkWeight;
			}

			@Override
			public boolean isClusteringEnabled() {
				return PoiOptionsBuilder.this.isClusteringEnabled;
			}

			@Override
			public double getTeleportationProbability() {
				return PoiOptionsBuilder.this.teleportationProbability;
			}

			@Override
			public double getInterLevelTeleportationProbability() {
				return PoiOptionsBuilder.this.interLevelTeleportationProbability;
			}
		};
	}
}
