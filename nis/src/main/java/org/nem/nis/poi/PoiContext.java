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

	public static final double TELEPORTATION_PROB = .75; // For NCDawareRank
	public static final double INVERSE_TELEPORTATION_PROB = .15; // For NCDawareRank
	public static final double INTER_LEVEL_TELEPORTATION_PROB = .1; // For NCDawareRank

	private final AccountProcessor accountProcessor;

	private final double inverseTeleportationProb;

	/**
	 * Creates a new context.
	 *
	 * @param accountStates The account states.
	 * @param height The current block height.
	 */
	public PoiContext(final Iterable<PoiAccountState> accountStates, final BlockHeight height, final GraphClusteringStrategy clusterer) {
		// (1) build the account vectors and matrices
		this.accountProcessor = new AccountProcessor(accountStates, height, clusterer);
		this.accountProcessor.process();

		// (2) build the teleportation vectors
		final TeleportationBuilder tb = new TeleportationBuilder(this.getPoiStartVector());
		this.inverseTeleportationProb = tb.inverseTeleportationProb;
	}

	/**
	 * Creates a new context with Scan clusterer.
	 *
	 * @param accountStates The account states.
	 * @param height The current block height.
	 */
	public PoiContext(final Iterable<PoiAccountState> accountStates, final BlockHeight height) {
		//		this(accounts, height, new Scan());
		this(accountStates, height, new FastScan());
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

	/**
	 * Gets the inverse teleportation probability.
	 *
	 * @return The inverse teleportation probability.
	 */
	public double getInverseTeleportationProb() {
		return this.inverseTeleportationProb;
	}

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

			long start = System.currentTimeMillis();
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
			long stop = System.currentTimeMillis();
			System.out.println("AccountProcessor ctor needed " + (stop - start) + "ms.");

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
			long start = System.currentTimeMillis();
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
			long stop = System.currentTimeMillis();
			System.out.println("AccountProcessor process needed " + (stop - start) + "ms.");
		}

		public void updateImportances(
				final ColumnVector pageRankVector,
				final ColumnVector importanceVector) {
			System.out.println("pageRankVector.size(): " + pageRankVector.size());
			System.out.println("this.accountInfos.size(): " + this.accountInfos.size());
			if (pageRankVector.size() != this.accountInfos.size()) {
				throw new IllegalArgumentException("ncd aware rank vector is an unexpected dimension");
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
			// TODO: BR: After unwinding a chain to height x in order to resolve a fork, the start vector is different
			//           from the start vector we use when growing an incomplete chain at height x. This results in
			//           a slightly different importance value. This in turn leads to a slightly different target for
			//           a block and could in theory lead to an unresolvable fork. Better use a constant vector for now.
			//TODO [M-BR]: isn't there a maximum number that can be unwound? Like every 1440 blocks?
			//If so, then maybe just store the poiStartVector every day, perhaps integrating this into snapshotting?
			// 20140817: BR -> M maximum number of blocks that can be unwound is 720.
			//                   If you store the poiStartVector every day you might have to calculate MANY (1440/31)
			//                   importances to get the exact poiStartVector. Starting with a constant vector is not that bad.

			// (1) Assign the start vector to the last page rank
			//			int i = 0;
			//			for (final PoiAccountInfo accountInfo : this.accountInfos) {
			//				final AccountImportance importance = accountInfo.getAccount().getImportanceInfo();
			//				this.poiStartVector.setAt(i, importance.getLastPageRank());
			//				++i;
			//			}
			//
			//			// (2) normalize the start vector
			//			if (this.poiStartVector.isZeroVector())
			//				this.poiStartVector.setAll(1.0);

			this.poiStartVector.setAll(1.0);
			this.poiStartVector.normalize();
		}

		private void createOutlinkMatrix() {
			// (1) add reverse links to allow net calculation
			long start = System.currentTimeMillis();
			for (final PoiAccountInfo accountInfo : this.accountInfos) {
				for (final WeightedLink link : accountInfo.getOutlinks()) {
					final PoiAccountInfo otherAccountInfo = this.addressToAccountInfoMap.get(link.getOtherAccountAddress());
					if (null == otherAccountInfo) {
						continue;
					}

					otherAccountInfo.addInlink(new WeightedLink(accountInfo.getState().getAddress(), link.getWeight()));
				}
			}
			long stop = System.currentTimeMillis();
			System.out.println("AccountProcessor createOutlinkMatrix add reverse links needed " + (stop - start) + "ms.");

			// (2) update the matrix with all net outflows
			start = System.currentTimeMillis();
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
			stop = System.currentTimeMillis();
			System.out.println("AccountProcessor createOutlinkMatrix create matrix needed " + (stop - start) + "ms.");

			//			try {
			//			this.outlinkMatrix.save("outlink.matrix");
			//		} catch (IOException e) {
			//			e.printStackTrace();
			//		}

			// We should create the NodeNeighborMap after removing negatives
			this.clusterAccounts();

			// Now we can build the inter-level proximity matrix (because we need directed edges for this)s
			start = System.currentTimeMillis();
			this.buildInterLevelProximityMatrix();
			stop = System.currentTimeMillis();
			System.out.println("AccountProcessor createOutlinkMatrix buildInterLevelProximityMatrix " + (stop - start) + "ms.");
		}

		private void clusterAccounts() {
			NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(this.outlinkMatrix);
			this.neighborhood = new Neighborhood(nodeNeighborMap, new DefaultSimilarityStrategy(nodeNeighborMap));
			long start = System.currentTimeMillis();
			this.clusteringResult = this.clusteringStrategy.cluster(neighborhood);
			long stop = System.currentTimeMillis();
			System.out.println("Clustering needed " + (stop - start) + "ms for " + neighborhood.size() + " accounts.");
		}

		private void buildInterLevelProximityMatrix() {
			this.interLevelMatrix = new InterLevelProximityMatrix(this.clusteringResult, this.neighborhood, this.outlinkMatrix);
		}
	}

	private static class TeleportationBuilder {

		private final double inverseTeleportationProb;

		public TeleportationBuilder(final ColumnVector importanceVector) {
			//			// (1) build the teleportation vector
			//			final int numAccounts = importanceVector.getSize();
			//
			//			// TODO: not sure if we should have non-zero teleportation for accounts that can't forage
			//			// TODO: After POI is up and running, we should try pruning accounts that can't forage
			//
			//			// Assign a value between .7 and .95 based on the amount of NEM in an account
			//			// It seems that more NEM = higher teleportation seems to work better
			//			// NOTE: at this point the importance vector contains normalized account balances
			//			final double maxImportance = importanceVector.max();
			//
			//			// calculate teleportation probabilities based on normalized amount of NEM owned
			//			final ColumnVector minProbVector = new ColumnVector(importanceVector.getSize());
			//			minProbVector.setAll(MIN_TELEPORTATION_PROB);
			//
			//			final double teleportationDelta = MAX_TELEPORTATION_PROB - MIN_TELEPORTATION_PROB;
			//			this.teleportationVector = minProbVector.add(importanceVector.multiply(teleportationDelta / maxImportance));
			//
			//			// (2) build the inverse teleportation vector: 1 - V(teleportation)
			//			final ColumnVector onesVector = new ColumnVector(numAccounts);
			//			onesVector.setAll(1.0);
			//			this.inverseTeleportationVector = onesVector.add(this.teleportationVector.multiply(-1));
			//
			//			// (3) Normalize by the number of accounts (1/N)
			//			final double numAccountNorm =  1d / numAccounts;
			//
			//			this.teleportationVector.multiply(numAccountNorm);
			//			this.inverseTeleportationVector.multiply(numAccountNorm);

			//			ColumnVector vector = new ColumnVector(importanceVector.size());
			//			vector.setAll(TELEPORTATION_PROB);
			////			this.teleportationVector = vector;
			//
			//			ColumnVector interLevelVector = new ColumnVector(importanceVector.size());
			//			interLevelVector.setAll(INTER_LEVEL_TELEPORTATION_PROB);
			//			this.interLevelTeleportationVector = interLevelVector;
			//
			//			final ColumnVector onesVector = new ColumnVector(importanceVector.size());
			//			onesVector.setAll(1.0);
			//			vector = onesVector.add(this.teleportationVector.add(this.interLevelTeleportationVector).multiply(-1));
			//			vector.scale(importanceVector.size());
			this.inverseTeleportationProb = (1.0 - (TELEPORTATION_PROB + INTER_LEVEL_TELEPORTATION_PROB)) / importanceVector.size();
		}
	}
}
