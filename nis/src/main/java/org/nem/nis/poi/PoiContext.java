package org.nem.nis.poi;

import org.nem.core.math.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.graph.*;
import org.nem.nis.secret.AccountImportance;

import java.util.*;

/**
 * A POI context.
 */
public class PoiContext {
	private static final double TELEPORTATION_PROB = .75; // For NCDawareRank
	private static final double INTER_LEVEL_TELEPORTATION_PROB = .1; // For NCDawareRank

	private final AccountProcessor accountProcessor;

	private final double teleportationProbability;
	private final double interLevelTeleportationProbability;
	private final double inverseTeleportationProbability;

	/**
	 * Creates a new context.
	 *
	 * @param accountStates The account states.
	 * @param height The current block height.
	 * @param clusterer The graph clusterer.
	 */
	public PoiContext(
			final Iterable<PoiAccountState> accountStates,
			final BlockHeight height,
			final GraphClusteringStrategy clusterer) {
		// (1) build the account vectors and matrices
		this.accountProcessor = new AccountProcessor(accountStates, height, clusterer);
		this.accountProcessor.process();

		// (2) set the teleportation values
		this.teleportationProbability = TELEPORTATION_PROB;
		this.interLevelTeleportationProbability = INTER_LEVEL_TELEPORTATION_PROB;
		this.inverseTeleportationProbability = (1.0 - TELEPORTATION_PROB - INTER_LEVEL_TELEPORTATION_PROB) / this.getPoiStartVector().size();
	}

	//region Getters

	/**
	 * Gets the vested balance vector.
	 *
	 * @return The vested balance vector.
	 */
	public ColumnVector getVestedBalanceVector() {
		return this.accountProcessor.vestedBalanceVector;
	}

	/**
	 * Gets the out-link score vector.
	 *
	 * @return The out-link vector.
	 */
	public ColumnVector getOutlinkScoreVector() {
		return this.accountProcessor.outlinkScoreVector;
	}

	/**
	 * Gets the poi start vector.
	 *
	 * @return The poi start vector.
	 */
	public ColumnVector getPoiStartVector() {
		return this.accountProcessor.poiStartVector;
	}

	//region teleportation probabilities

	/**
	 * Gets the teleportation probability.
	 *
	 * @return The teleportation probability.
	 */
	public double getTeleportationProbability() {
		return this.teleportationProbability;
	}

	/**
	 * Gets the inter-level teleportation probability.
	 *
	 * @return The inter-level teleportation probability.
	 */
	public double getInterLevelTeleportationProbability() {
		return this.interLevelTeleportationProbability;
	}

	/**
	 * Gets the inverse teleportation probability.
	 *
	 * @return The inverse teleportation probability.
	 */
	public double getInverseTeleportationProbability() {
		return this.inverseTeleportationProbability;
	}

	//endregion

	/**
	 * Gets the dangle indexes.
	 *
	 * @return The dangle indexes.
	 */
	public List<Integer> getDangleIndexes() {
		return this.accountProcessor.dangleIndexes;
	}

	/**
	 * Gets the dangle vector, where an element has a 0 value if the corresponding account is dangling.
	 *
	 * @return The dangle vector.
	 */
	public ColumnVector getDangleVector() {
		return this.accountProcessor.dangleVector;
	}

	/**
	 * Gets the out-link matrix.
	 *
	 * @return The out-link matrix.
	 */
	public SparseMatrix getOutlinkMatrix() {
		return this.accountProcessor.outlinkMatrix;
	}

	/**
	 * Gets the inter-level proximity matrix.
	 *
	 * @return The inter-level proximity matrix.
	 */
	public InterLevelProximityMatrix getInterLevelMatrix() {
		return this.accountProcessor.interLevelMatrix;
	}

	/**
	 * Gets the clustering result.
	 *
	 * @return The clustering result.
	 */
	public ClusteringResult getClusteringResult() {
		return this.accountProcessor.clusteringResult;
	}

	//endregion

	/**
	 * Updates the importance information for all foraging-eligible accounts.
	 *
	 * @param pageRankVector The calculated page ranks.
	 * @param importanceVector The calculated importances.
	 */
	public void updateImportances(
			final ColumnVector pageRankVector,
			final ColumnVector importanceVector) {
		this.accountProcessor.updateImportances(pageRankVector, importanceVector);
	}

	private static class AccountProcessor {
		private final BlockHeight height;
		private final List<Integer> dangleIndexes;
		private final ColumnVector dangleVector;
		private final ColumnVector vestedBalanceVector;
		private final ColumnVector poiStartVector;
		private final ColumnVector outlinkScoreVector;
		private SparseMatrix outlinkMatrix;
		private InterLevelProximityMatrix interLevelMatrix;
		private final GraphClusteringStrategy clusteringStrategy;
		private ClusteringResult clusteringResult;
		private Neighborhood neighborhood;

		private final List<PoiAccountInfo> accountInfos = new ArrayList<>();
		private final Map<Address, PoiAccountInfo> addressToAccountInfoMap = new HashMap<>();
		private final Map<Address, Integer> addressToIndexMap = new HashMap<>();

		public AccountProcessor(final Iterable<PoiAccountState> accountStates, final BlockHeight height, final GraphClusteringStrategy clusteringStrategy) {
			this.height = height;
			this.dangleIndexes = new ArrayList<>();
			this.clusteringStrategy = clusteringStrategy;

			int i = 0;
			for (final PoiAccountState accountState : accountStates) {
				final PoiAccountInfo accountInfo = new PoiAccountInfo(i, accountState, height);
				if (!accountInfo.canHarvest()) {
					continue;
				}

				final Address address = accountState.getAddress();
				this.addressToAccountInfoMap.put(address, accountInfo);
				this.addressToIndexMap.put(address, i);

				this.accountInfos.add(accountInfo);
				++i;
			}

			if (0 == i) {
				throw new IllegalArgumentException("there aren't any harvesting eligible accounts");
			}

			this.dangleVector = new ColumnVector(i);
			this.dangleVector.setAll(1);

			this.vestedBalanceVector = new ColumnVector(i);
			this.poiStartVector = new ColumnVector(i);
			this.outlinkScoreVector = new ColumnVector(i);
		}

		public void process() {
			// (1) go through all accounts and set the vested balances
			int i = 0;
			int numOutlinks = 0;
			for (final PoiAccountInfo accountInfo : this.accountInfos) {
				final PoiAccountState accountState = accountInfo.getState();
				numOutlinks += accountState.getImportanceInfo().getOutlinksSize(this.height);
				this.vestedBalanceVector.setAt(i, accountState.getWeightedBalances().getVested(this.height).getNumMicroNem());
				++i;
			}

			this.outlinkMatrix = new SparseMatrix(i, i, numOutlinks < i ? 1 : numOutlinks / i);
			this.createOutlinkMatrix();

			// (2) create the start vector
			this.createStartVector();
		}

		public void updateImportances(
				final ColumnVector pageRankVector,
				final ColumnVector importanceVector) {
			if (pageRankVector.size() != this.accountInfos.size()) {
				throw new IllegalArgumentException("page rank vector is an unexpected dimension");
			}

			if (importanceVector.size() != this.accountInfos.size()) {
				throw new IllegalArgumentException("importance vector is an unexpected dimension");
			}

			int i = 0;
			for (final PoiAccountInfo accountInfo : this.accountInfos) {
				final AccountImportance importance = accountInfo.getState().getImportanceInfo();
				importance.setLastPageRank(pageRankVector.getAt(i));
				importance.setImportance(this.height, importanceVector.getAt(i));
				++i;
			}
		}

		private void createStartVector() {
			this.poiStartVector.setAll(1.0);
			this.poiStartVector.normalize();
		}

		private void createOutlinkMatrix() {
			// (1) add reverse links to allow net calculation
			for (final PoiAccountInfo accountInfo : this.accountInfos) {
				for (final WeightedLink link : accountInfo.getOutlinks()) {
					final PoiAccountInfo otherAccountInfo = this.addressToAccountInfoMap.get(link.getOtherAccountAddress());
					if (null == otherAccountInfo) {
						continue;
					}

					otherAccountInfo.addInlink(new WeightedLink(accountInfo.getState().getAddress(), link.getWeight()));
				}
			}

			// (2) update the matrix with all net outflows
			for (final PoiAccountInfo accountInfo : this.accountInfos) {
				for (final WeightedLink link : accountInfo.getNetOutlinks()) {
					final Integer rowIndex = this.addressToIndexMap.get(link.getOtherAccountAddress());
					if (null == rowIndex) {
						continue;
					}

					this.outlinkMatrix.incrementAt(rowIndex, accountInfo.getIndex(), link.getWeight());
				}

				// update the outlink score
				final int rowIndex = accountInfo.getIndex();
				final double outlinkScore = accountInfo.getNetOutlinkScore();
				if (0.0 != outlinkScore) {
					this.outlinkScoreVector.setAt(rowIndex, outlinkScore);
				}
			}

			this.outlinkMatrix.removeNegatives();

			// At this point the outlink matrix gets finalized.
			// This is the point where the dangle indices should be calculated, not earlier!
			// The normalizeColumns() method returns the dangle indices because during normalization
			// the column sums need to be calculated anyway.
			this.dangleIndexes.addAll(this.outlinkMatrix.normalizeColumns());

			// We should create the NodeNeighborMap after removing negatives
			this.clusterAccounts();

			// Now we can build the inter-level proximity matrix (because we need directed edges for this)s
			this.buildInterLevelProximityMatrix();
		}

		private void clusterAccounts() {
			final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(this.outlinkMatrix);
			this.neighborhood = new Neighborhood(nodeNeighborMap, new DefaultSimilarityStrategy(nodeNeighborMap));
			this.clusteringResult = this.clusteringStrategy.cluster(this.neighborhood);
		}

		private void buildInterLevelProximityMatrix() {
			this.interLevelMatrix = new InterLevelProximityMatrix(this.clusteringResult, this.neighborhood, this.outlinkMatrix);
		}
	}
}
