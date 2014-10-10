package org.nem.nis.poi;

import org.nem.core.model.primitive.Amount;
import org.nem.nis.poi.graph.*;

/**
 * A builder for creating poi options.
 */
public class PoiOptionsBuilder {
	private Amount minHarvesterBalance = Amount.fromNem(1000);
	private Amount minOutlinkWeight = Amount.ZERO;
	private double negativeOutlinkWeight = 0.2;
	private double outlierWeight = 1.0;
	private double teleportationProbability = .75; // For NCDawareRank
	private double interLevelTeleportationProbability = .1; // For NCDawareRank
	private GraphClusteringStrategy clusteringStrategy = new FastScanClusteringStrategy();
	private int mu = 3;
	private double epsilon = 0.65;

	//region weights

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

	//endregion

	//region clustering

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

	//endregion

	//region teleportation

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

	//endregion

//	//negativeOutlinkWeights = [0, 20, 40, 60, 80, 100]
//	//outlierWeights = [0.85, 0.9, 0.95]

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
