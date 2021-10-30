package org.nem.nis.pox.poi;

import org.nem.core.math.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.utils.FormatUtils;
import org.nem.nis.harvesting.CanHarvestPredicate;
import org.nem.nis.pox.poi.graph.*;
import org.nem.nis.state.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * A POI context.
 */
public class PoiContext {
	private static final Logger LOGGER = Logger.getLogger(PoiContext.class.getName());

	private final AccountProcessor accountProcessor;

	/**
	 * Creates a new context.
	 *
	 * @param accountStates The account states.
	 * @param height The current block height.
	 * @param options The poi options.
	 */
	public PoiContext(final Iterable<AccountState> accountStates, final BlockHeight height, final PoiOptions options) {
		// (1) build the account vectors and matrices
		this.accountProcessor = new AccountProcessor(accountStates, height, options);
		this.accountProcessor.process();
	}

	// region getters - vectors

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

	/**
	 * Gets the outlier vector.
	 *
	 * @return The outlier vector.
	 */
	public ColumnVector getOutlierVector() {
		return this.accountProcessor.outlierVector;
	}

	/**
	 * Gets the graph weight vector.
	 *
	 * @return The graph weight vector.
	 */
	public ColumnVector getGraphWeightVector() {
		return this.accountProcessor.graphWeightVector;
	}

	// endregion

	// region getters - dangle indexes

	/**
	 * Gets the dangle indexes.
	 *
	 * @return The dangle indexes.
	 */
	public List<Integer> getDangleIndexes() {
		return this.accountProcessor.dangleIndexes;
	}

	// endregion

	// region getters - matrices

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

	// endregion

	// region getters - clustering result

	/**
	 * Gets the clustering result.
	 *
	 * @return The clustering result.
	 */
	public ClusteringResult getClusteringResult() {
		return this.accountProcessor.clusteringResult;
	}

	// endregion

	/**
	 * Updates the importance information for all harvesting-eligible accounts.
	 *
	 * @param pageRankVector The calculated page ranks.
	 * @param importanceVector The calculated importances.
	 */
	public void updateImportances(final ColumnVector pageRankVector, final ColumnVector importanceVector) {
		this.accountProcessor.updateImportances(pageRankVector, importanceVector);
	}

	private static class AccountProcessor {
		private final BlockHeight height;
		private final List<Integer> dangleIndexes;
		private final PoiOptions options;

		private final ColumnVector vestedBalanceVector;
		private final ColumnVector poiStartVector;
		private final ColumnVector outlinkScoreVector;
		private final ColumnVector outlierVector;
		private final ColumnVector graphWeightVector;
		private SparseMatrix outlinkMatrix;
		private InterLevelProximityMatrix interLevelMatrix;
		private ClusteringResult clusteringResult;
		private Neighborhood neighborhood;

		private final List<PoiAccountInfo> accountInfos = new ArrayList<>();
		private final Map<Address, PoiAccountInfo> addressToAccountInfoMap = new HashMap<>();
		private final Map<Address, Integer> addressToIndexMap = new HashMap<>();

		public AccountProcessor(final Iterable<AccountState> accountStates, final BlockHeight height, final PoiOptions options) {
			this.height = height;
			this.dangleIndexes = new ArrayList<>();
			this.options = options;

			int i = 0;
			final CanHarvestPredicate canHarvestPredicate = new CanHarvestPredicate(this.options.getMinHarvesterBalance());
			for (final AccountState accountState : accountStates) {
				if (!canHarvestPredicate.canHarvest(accountState, height)) {
					continue;
				}

				final PoiAccountInfo accountInfo = new PoiAccountInfo(i, accountState, height);
				final Address address = accountState.getAddress();
				this.addressToAccountInfoMap.put(address, accountInfo);
				this.addressToIndexMap.put(address, i);

				this.accountInfos.add(accountInfo);
				++i;
			}

			if (0 == i) {
				throw new IllegalArgumentException("there aren't any harvesting eligible accounts");
			}

			this.vestedBalanceVector = new ColumnVector(i);
			this.poiStartVector = new ColumnVector(i);
			this.outlinkScoreVector = new ColumnVector(i);
			this.outlierVector = new ColumnVector(i);
			this.graphWeightVector = new ColumnVector(i);
		}

		public void process() {
			// (1) go through all accounts and set the vested balances
			int i = 0;
			int numOutlinks = 0;
			for (final PoiAccountInfo accountInfo : this.accountInfos) {
				final AccountState accountState = accountInfo.getState();
				numOutlinks += accountState.getImportanceInfo().getOutlinksSize(this.height);
				this.vestedBalanceVector.setAt(i, accountState.getWeightedBalances().getVested(this.height).getNumMicroNem());
				++i;
			}

			this.outlinkMatrix = new SparseMatrix(i, i, numOutlinks < i ? 1 : numOutlinks / i);
			this.createOutlinkMatrix();

			// (2) initialize the start vector
			this.initializeStartVector();
		}

		public void updateImportances(final ColumnVector pageRankVector, final ColumnVector importanceVector) {
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

				// on machines that do not support historical information, each HistoricalImportances will only contain a
				// single entry in-between pruning
				final HistoricalImportances historicalImportances = accountInfo.getState().getHistoricalImportances();
				historicalImportances
						.addHistoricalImportance(new AccountImportance(this.height, importanceVector.getAt(i), pageRankVector.getAt(i)));
				++i;
			}
		}

		private void initializeStartVector() {
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
				final double score = accountInfo.getNetOutlinkScore();
				final double multiplier = score < 0 ? this.options.getNegativeOutlinkWeight() : 1.0;
				final double outlinkScore = score * multiplier;
				if (0.0 != outlinkScore) {
					this.outlinkScoreVector.setAt(rowIndex, outlinkScore);
				}
			}

			this.outlinkMatrix.removeLessThan(this.options.getMinOutlinkWeight().getNumMicroNem());

			// At this point the outlink matrix gets finalized.
			// This is the point where the dangle indices should be calculated, not earlier!
			// The normalizeColumns() method returns the dangle indices because during normalization
			// the column sums need to be calculated anyway.
			final Collection<Integer> dangleIndexes = this.outlinkMatrix.normalizeColumns();
			this.dangleIndexes.addAll(dangleIndexes);

			if (!this.options.isClusteringEnabled()) {
				LOGGER.info("clustering is bypassed");
				return;
			}

			// We should cluster the accounts
			this.clusterAccounts();

			// Now we can build the inter-level proximity matrix (because we need directed edges for this)
			this.buildInterLevelProximityMatrix();
		}

		private void clusterAccounts() {
			final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(this.outlinkMatrix);
			this.neighborhood = new Neighborhood(nodeNeighborMap, new StructuralSimilarityStrategy(nodeNeighborMap),
					this.options.getMuClusteringValue(), this.options.getEpsilonClusteringValue());
			this.clusteringResult = this.options.getClusteringStrategy().cluster(this.neighborhood);
			LOGGER.info(String.format("clustering completed: { clusters: %d (average size: %s), hubs: %d, outliers: %d }",
					this.clusteringResult.getClusters().size(), FormatUtils.format(this.clusteringResult.getAverageClusterSize(), 2),
					this.clusteringResult.getHubs().size(), this.clusteringResult.getOutliers().size()));

			this.graphWeightVector.setAll(1.0);
			for (final Cluster cluster : this.clusteringResult.getOutliers()) {
				final int outlierId = cluster.getId().getRaw();
				this.outlierVector.setAt(outlierId, 1.0);
				this.graphWeightVector.setAt(outlierId, this.options.getOutlierWeight());
			}
		}

		private void buildInterLevelProximityMatrix() {
			this.interLevelMatrix = new InterLevelProximityMatrix(this.clusteringResult, this.neighborhood, this.outlinkMatrix);
		}
	}
}
