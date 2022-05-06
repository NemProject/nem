package org.nem.nis.pox.poi;

import org.nem.core.model.primitive.*;
import org.nem.nis.pox.poi.graph.*;

/**
 * A builder for creating poi options.
 */
public class PoiOptionsBuilder {
	private Amount minHarvesterBalance = Amount.fromNem(10000);
	private Amount minOutlinkWeight = Amount.fromNem(1000);
	private double negativeOutlinkWeight = 0.6;
	private double outlierWeight = 0.9;
	private double teleportationProbability = 0.7; // For NCDawareRank
	private double interLevelTeleportationProbability = .1; // For NCDawareRank
	private GraphClusteringStrategy clusteringStrategy = new FastScanClusteringStrategy();
	private int mu = 4;
	private double epsilon = 0.3;

	/**
	 * Creates a new options builder.
	 */
	public PoiOptionsBuilder() {
		this(BlockHeight.ONE);
	}

	/**
	 * Creates a new options builder defaulted to options at the specified height.
	 *
	 * @param height The block height.
	 */
	@SuppressWarnings("UnusedParameters")
	public PoiOptionsBuilder(final BlockHeight height) {
	}

	// region weights

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
	 * Sets the weight given to (net) negative outlinks.
	 *
	 * @param weight The weight.
	 */
	public void setNegativeOutlinkWeight(final double weight) {
		this.negativeOutlinkWeight = weight;
	}

	/**
	 * Sets the weight given to outlier nodes.
	 *
	 * @param weight The weight.
	 */
	public void setOutlierWeight(final double weight) {
		this.outlierWeight = weight;
	}

	// endregion

	// region clustering

	/**
	 * Sets the graph clustering strategy.
	 *
	 * @param strategy The clustering strategy.
	 */
	public void setClusteringStrategy(final GraphClusteringStrategy strategy) {
		this.clusteringStrategy = strategy;
	}

	/**
	 * Sets the mu clustering variable.
	 *
	 * @param mu The mu value.
	 */
	public void setMuClusteringValue(final int mu) {
		this.mu = mu;
	}

	/**
	 * Sets the epsilon clustering variable.
	 *
	 * @param epsilon The epsilon value.
	 */
	public void setEpsilonClusteringValue(final double epsilon) {
		this.epsilon = epsilon;
	}

	// endregion

	// region teleportation

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

	// endregion

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
			public double getNegativeOutlinkWeight() {
				return PoiOptionsBuilder.this.negativeOutlinkWeight;
			}

			@Override
			public double getOutlierWeight() {
				return PoiOptionsBuilder.this.outlierWeight;
			}

			@Override
			public boolean isClusteringEnabled() {
				return null != this.getClusteringStrategy();
			}

			@Override
			public GraphClusteringStrategy getClusteringStrategy() {
				return PoiOptionsBuilder.this.clusteringStrategy;
			}

			@Override
			public int getMuClusteringValue() {
				return PoiOptionsBuilder.this.mu;
			}

			@Override
			public double getEpsilonClusteringValue() {
				return PoiOptionsBuilder.this.epsilon;
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
