package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.core.math.SparseMatrix;
import org.nem.core.model.Account;
import org.nem.core.model.AccountLink;
import org.nem.core.model.Address;
import org.nem.core.model.BlockHeight;

import java.util.*;

/**
 * A POI context.
 */
public class PoiContext {

	private static final double TELEPORTATION_PROB = .85;

	private final List<Integer> dangleIndexes;
	private final ColumnVector dangleVector;
	private final ColumnVector vestedBalanceVector;
	private final ColumnVector importanceVector;
	private final ColumnVector outlinkScoreVector;
	private final SparseMatrix outlinkMatrix;

	private final ColumnVector teleportationVector;
	private final ColumnVector inverseTeleportationVector;

	/**
	 * Creates a new context.
	 *
	 * @param accounts The accounts.
	 * @param numAccounts The number of accounts.
	 * @param height The current block height.
	 */
	public PoiContext(final Iterable<Account> accounts, final int numAccounts, final BlockHeight height) {
		// (1) build the account vectors and matrices
		final AccountProcessor ap = new AccountProcessor(numAccounts);
		ap.process(accounts, height);
		this.dangleIndexes = ap.dangleIndexes;
		this.dangleVector = ap.dangleVector;
		this.vestedBalanceVector = ap.vestedBalanceVector;
		this.outlinkScoreVector = ap.outlinkScoreVector;
		this.importanceVector = ap.importanceVector;
		this.outlinkMatrix = ap.outlinkMatrix;

		// (2) build the teleportation vectors
		final TeleportationBuilder tb = new TeleportationBuilder(this.importanceVector);
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
		return this.vestedBalanceVector;
	}

	/**
	 * Gets the out-link score vector.
	 *
	 * @return The out-link vector.
	 */
	public ColumnVector getOutlinkScoreVector() {
		return this.outlinkScoreVector;
	}

	/**
	 * Gets the importance vector.
	 *
	 * @return The importance vector.
	 */
	public ColumnVector getImportanceVector() {
		return this.importanceVector;
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
		return this.dangleIndexes;
	}

	/**
	 * Gets the dangle vector, where an element has a 0 value if the corresponding account is dangling.
	 *
	 * @return The dangle vector.
	 */
	public ColumnVector getDangleVector() {
		return this.dangleVector;
	}

	/**
	 * Gets the out-link matrix.
	 *
	 * @return The out-link matrix.
	 */
	public SparseMatrix getOutlinkMatrix() {
		return this.outlinkMatrix;
	}

	//endregion

	private static class AccountProcessor {

		private final List<Integer> dangleIndexes;
		private final ColumnVector dangleVector;
		private final ColumnVector vestedBalanceVector;
		private ColumnVector importanceVector;
		private final ColumnVector outlinkScoreVector;
		private SparseMatrix outlinkMatrix;

		private final List<PoiAccountInfo> accountInfos = new ArrayList<>();
		private final Map<Address, Integer> addressToIndexMap = new HashMap<>();

		public AccountProcessor(final int numAccounts) {
			this.dangleIndexes = new ArrayList<>();

			this.dangleVector = new ColumnVector(numAccounts);
			this.dangleVector.setAll(1);

			this.vestedBalanceVector = new ColumnVector(numAccounts);
			this.importanceVector = new ColumnVector(numAccounts);
			this.outlinkScoreVector = new ColumnVector(numAccounts);
		}

		public void process(final Iterable<Account> accounts, final BlockHeight height) {
			// (1) go through all accounts and initialize all vectors
			int i = 0;
			int numOutlinks = 0;
			for (final Account account : accounts) {
				final PoiAccountInfo accountInfo = new PoiAccountInfo(i, account, height);
				numOutlinks += account.getImportanceInfo().getOutlinksSize(height);
				// TODO: to simplify the calculation, should we exclude accounts that can't forage?
				// TODO: (this should shrink the matrix size)
				// TODO: I would recommend playing around with this after we get POI working initially
				//	 if (!accountInfo.canForage())
				//	 continue;

				this.addressToIndexMap.put(account.getAddress(), i);

				this.accountInfos.add(accountInfo);
				this.vestedBalanceVector.setAt(i, account.getWeightedBalances().getVested(height).getNumMicroNem());
				this.outlinkScoreVector.setAt(i, accountInfo.getOutlinkScore());

				if (!accountInfo.hasOutlinks())
					this.dangleIndexes.add(i);

				++i;
			}

			this.outlinkMatrix = new SparseMatrix(i, i, numOutlinks < i ? 1 : numOutlinks / i);
			this.createOutlinkMatrix(height);

			// Initially set importance to the row sum vector of the outlink matrix
			// TODO: Is there a better way to estimate the eigenvector
			// TODO: maybe save from previous block
			this.createImportanceVector();
		}
		
		private void createImportanceVector() {
			// (1) Assign the row sum of the outlinkMatrix to the components
			this.importanceVector = this.outlinkMatrix.getRowSumVector();
			
			// (2) normalize the importance vector
			this.importanceVector.normalize();			
		}

		private void createOutlinkMatrix(BlockHeight height) {
			for (final PoiAccountInfo accountInfo : this.accountInfos) {

				if (!accountInfo.hasOutlinks())
					continue;

				final ColumnVector outlinkWeights = accountInfo.getOutlinkWeights();
				final Iterator<AccountLink> accountLinkIterator = accountInfo.getAccount()
						.getImportanceInfo()
						.getOutlinksIterator(height);
				for (int i = 0; i < outlinkWeights.size(); ++i) {
					final AccountLink outlink = accountLinkIterator.next();
					int rowIndex = this.addressToIndexMap.get(outlink.getOtherAccountAddress());
					this.outlinkMatrix.incrementAt(rowIndex, accountInfo.getIndex(), outlinkWeights.getAt(i));
					//decrement other end of transaction
					this.outlinkMatrix.decrementAt(accountInfo.getIndex(), rowIndex, outlinkWeights.getAt(i));
				}
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
