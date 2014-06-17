package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.core.math.SparseMatrix;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;

import java.util.*;

/**
 * A POI context.
 */
public class PoiContext {

	private static final double TELEPORTATION_PROB = .85;

	private final AccountProcessor accountProcessor;

	private final ColumnVector teleportationVector;
	private final ColumnVector inverseTeleportationVector;

	/**
	 * Creates a new context.
	 *
	 * @param accounts The accounts.
	 * @param height The current block height.
	 */
	public PoiContext(final Iterable<Account> accounts, final BlockHeight height) {
		// (1) build the account vectors and matrices
		this.accountProcessor = new AccountProcessor(accounts, height);
		this.accountProcessor.process();

		// (2) build the teleportation vectors
		final TeleportationBuilder tb = new TeleportationBuilder(this.getPoiStartVector());
		this.teleportationVector = tb.teleportationVector;
		this.inverseTeleportationVector = tb.inverseTeleportationVector;
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
	 * Gets the teleportation vector.
	 *
	 * @return The teleportation vector.
	 */
	public ColumnVector getTeleportationVector() {
		return this.teleportationVector;
	}

	/**
	 * Gets the inverse teleportation vector.
	 *
	 * @return The inverse teleportation vector.
	 */
	public ColumnVector getInverseTeleportationVector() {
		return this.inverseTeleportationVector;
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

		private final List<PoiAccountInfo> accountInfos = new ArrayList<>();
		private final Map<Address, PoiAccountInfo> addressToAccountInfoMap = new HashMap<>();
		private final Map<Address, Integer> addressToIndexMap = new HashMap<>();

		public AccountProcessor(final Iterable<Account> accounts, final BlockHeight height) {
			this.height = height;
			this.dangleIndexes = new ArrayList<>();

			int i = 0;
			for (final Account account : accounts) {
				final PoiAccountInfo accountInfo = new PoiAccountInfo(i, account, height);
				if (!accountInfo.canForage())
					continue;

				this.addressToAccountInfoMap.put(account.getAddress(), accountInfo);
				this.addressToIndexMap.put(account.getAddress(), i);

				this.accountInfos.add(accountInfo);
				++i;
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
				final Account account = accountInfo.getAccount();
				numOutlinks += account.getImportanceInfo().getOutlinksSize(this.height);
				this.vestedBalanceVector.setAt(i, account.getWeightedBalances().getVested(this.height).getNumMicroNem());
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
			if (pageRankVector.size() != this.accountInfos.size())
				throw new IllegalArgumentException("page rank vector is an unexpected dimension");

			if (importanceVector.size() != this.accountInfos.size())
				throw new IllegalArgumentException("importance vector is an unexpected dimension");

			int i = 0;
			for (final PoiAccountInfo accountInfo : this.accountInfos) {
				final AccountImportance importance = accountInfo.getAccount().getImportanceInfo();
				importance.setLastPageRank(pageRankVector.getAt(i));
				importance.setImportance(this.height, importanceVector.getAt(i));
				++i;
			}
		}

		private void createStartVector() {
			// (1) Assign the start vector to the last page rank
			int i = 0;
			for (final PoiAccountInfo accountInfo : this.accountInfos) {
				final AccountImportance importance = accountInfo.getAccount().getImportanceInfo();
				this.poiStartVector.setAt(i, importance.getLastPageRank());
				++i;
			}

			// (2) normalize the start vector
			if (this.poiStartVector.isZeroVector())
				this.poiStartVector.setAll(1.0);

			this.poiStartVector.normalize();
		}

		private void createOutlinkMatrix() {
			// (1) add reverse links to allow net calculation
			for (final PoiAccountInfo accountInfo : this.accountInfos) {
				for (final WeightedLink link : accountInfo.getOutlinks()) {
					final PoiAccountInfo otherAccountInfo = this.addressToAccountInfoMap.get(link.getOtherAccountAddress());
					if (null == otherAccountInfo)
						continue;

					otherAccountInfo.addInlink(new WeightedLink(accountInfo.getAccount().getAddress(), link.getWeight()));
				}
			}

			// (2) update the matrix with all net outflows
			for (final PoiAccountInfo accountInfo : this.accountInfos) {
				for (final WeightedLink link : accountInfo.getNetOutlinks()) {
					final Integer rowIndex = this.addressToIndexMap.get(link.getOtherAccountAddress());
					if (null == rowIndex)
						continue;

					this.outlinkMatrix.incrementAt(rowIndex, accountInfo.getIndex(), link.getWeight());
				}

				// update the outlink score
				int rowIndex = accountInfo.getIndex();
				final double outlinkScore = accountInfo.getNetOutlinkScore();
				if (0.0 == outlinkScore)
					this.dangleIndexes.add(rowIndex);
				else
					this.outlinkScoreVector.setAt(rowIndex, outlinkScore);
			}

			this.outlinkMatrix.removeNegatives();
			this.outlinkMatrix.normalizeColumns();
		}
	}

	private static class TeleportationBuilder {

		private final ColumnVector teleportationVector;
		private final ColumnVector inverseTeleportationVector;

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

			ColumnVector vector = new ColumnVector(importanceVector.size());
			vector.setAll(TELEPORTATION_PROB);
			this.teleportationVector = vector;
			final ColumnVector onesVector = new ColumnVector(importanceVector.size());
			onesVector.setAll(1.0);
			vector = onesVector.add(this.teleportationVector.multiply(-1));
			vector.scale(importanceVector.size());
			this.inverseTeleportationVector = vector;
		}
	}
}
