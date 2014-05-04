package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.core.math.Matrix;
import org.nem.core.model.Account;
import org.nem.core.model.AccountLink;
import org.nem.core.model.Address;
import org.nem.core.model.BlockHeight;

import javax.persistence.Column;
import java.util.*;

/**
 * A POI context.
 */
public class PoiContext {

	private static final double MIN_TELEPORTATION_PROB = .7;
	private static final double MAX_TELEPORTATION_PROB = .95;

	private final List<Integer> dangleIndexes;
	private final ColumnVector dangleVector;
	private final ColumnVector coinDaysVector;
	private final ColumnVector importanceVector;
	private final ColumnVector teleportationVector;
	private final ColumnVector inverseTeleportationVector;
	private final ColumnVector outLinkScoreVector;
	private final Matrix outLinkMatrix;

	/**
	 * Creates a new context.
	 *
	 * @param accounts The accounts.
	 * @param numAccounts The number of accounts.
	 * @param height The current block height.
	 */
	public PoiContext(final Iterable<Account> accounts, final int numAccounts, final BlockHeight height) {
		// TODO: this function should be broken up a bit
		List<PoiAccountInfo> accountInfos = new ArrayList<>();
		Map<Address, Integer> addressToIndexMap = new HashMap<>();
		this.dangleIndexes = new ArrayList<>();

		this.dangleVector = new ColumnVector(numAccounts);
		this.dangleVector.setAll(1);

		this.coinDaysVector = new ColumnVector(numAccounts);
		this.importanceVector = new ColumnVector(numAccounts);
		this.outLinkScoreVector = new ColumnVector(numAccounts);

		// (1) go through all accounts and initialize all vectors
		int i = 0;
		for (final Account account : accounts) {
			final PoiAccountInfo accountInfo = new PoiAccountInfo(i, account);
			// TODO: to simplify the calculation, should we exclude accounts that can't forage?
			// TODO: (this should shrink the matrix size)
			//	 if (!accountInfo.canForage())
			//	 continue;

			addressToIndexMap.put(account.getAddress(), i);

			accountInfos.add(accountInfo);
			this.coinDaysVector.setAt(i, account.getCoinDayWeightedBalance(height).getNumNem());
			this.outLinkScoreVector.setAt(i, accountInfo.getOutLinkScore());

			// initially set importance to account balance
			this.importanceVector.setAt(i, account.getBalance().getNumNem());

			if (!accountInfo.hasOutLinks()) {
				this.dangleIndexes.add(i);
				this.dangleVector.setAt(i, 0);
			}

			++i;
		}

		// (2) normalize the importance vector
		this.importanceVector.normalize();

		// (3) build the teleportation vector
		this.teleportationVector = createTeleportationVector();
		final ColumnVector onesVector = new ColumnVector(numAccounts);
		onesVector.setAll(1.0);
		this.inverseTeleportationVector = onesVector.add(this.teleportationVector.multiply(-1));

		// (4) build the out-link matrix
		this.outLinkMatrix = new Matrix(numAccounts, numAccounts);
		for (final PoiAccountInfo accountInfo : accountInfos) {

			if (!accountInfo.hasOutLinks())
				continue;

			final ColumnVector outLinkWeights = accountInfo.getOutLinkWeights();
			for (int j = 0; j < outLinkWeights.getSize(); ++j) {
				// TODO: using a hash-map for this will be slow
				final AccountLink outLink = accountInfo.getAccount().getOutlinks().get(j);
				int rowIndex = addressToIndexMap.get(outLink.getOtherAccount().getAddress());
				this.outLinkMatrix.setAt(rowIndex, accountInfo.getIndex(), outLinkWeights.getAt(j));
			}
		}
	}

	/**
	 * Gets the coin days vector.
	 *
	 * @return The coin days vector.
	 */
	public ColumnVector getCoinDaysVector() {
		return this.coinDaysVector;
	}

	/**
	 * Gets the out-link score vector.
	 *
	 * @return The out-link vector.
	 */
	public ColumnVector getOutLinkScoreVector() {
		return this.outLinkScoreVector;
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
	public Matrix getOutLinkMatrix() {
		return this.outLinkMatrix;
	}

	private ColumnVector createTeleportationVector() {
		// TODO: not sure if we should have non-zero teleportation for accounts that can't forage

		// Assign a value between .7 and .95 based on the amount of NEM in an account
		// It seems that more NEM = higher teleportation seems to work better
		// NOTE: at this point the importance vector contains normalized account balances
		final double maxImportance = this.importanceVector.max();

		// calculate teleportation probabilities based on normalized amount of NEM owned
		final ColumnVector teleportationVector = new ColumnVector(this.importanceVector.getSize());
		teleportationVector.setAll(MIN_TELEPORTATION_PROB);

		final double teleportationDelta = MAX_TELEPORTATION_PROB - MIN_TELEPORTATION_PROB;
		return teleportationVector.add(this.importanceVector.multiply(teleportationDelta / maxImportance));
	}
}
