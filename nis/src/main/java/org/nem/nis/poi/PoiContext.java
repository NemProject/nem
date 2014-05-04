package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.Account;
import org.nem.core.model.Address;
import org.nem.core.model.BlockHeight;

import java.util.*;

/**
 * A POI context.
 */
public class PoiContext {

	private static final double MIN_TELEPORTATION_PROB = .7;
	private static final double MAX_TELEPORTATION_PROB = .95;

	private final List<PoiAccountInfo> accountInfos;
	private final Map<Address, Integer> addressToIndexMap;
	private final List<Integer> dangleIndexes;
	private final ColumnVector coinDaysVector;
	private final ColumnVector importanceVector;
	private final ColumnVector teleportationVector;

	/**
	 * Creates a new context.
	 *
	 * @param accounts The accounts.
	 * @param numAccounts The number of accounts.
	 * @param height The current block height.
	 */
	public PoiContext(final Iterable<Account> accounts, final int numAccounts, final BlockHeight height) {
		this.accountInfos = new ArrayList<>();
		this.addressToIndexMap = new HashMap<>();
		this.dangleIndexes = new ArrayList<>();

		this.coinDaysVector = new ColumnVector(numAccounts);
		this.importanceVector = new ColumnVector(numAccounts);

		// go through all accounts and initialize everything
		int i = 0;
		for (final Account account : accounts) {
			final PoiAccountInfo accountInfo = new PoiAccountInfo(i, account);
			// TODO: to simplify the calculation, should we exclude accounts that can't forage?
			// TODO: (this should shrink the matrix size)
			//	 if (!accountInfo.canForage())
			//	 continue;

			this.addressToIndexMap.put(account.getAddress(), i);

			this.accountInfos.add(new PoiAccountInfo(i, account));
			coinDaysVector.setAt(i, account.getCoinDayWeightedBalance(height).getNumNem());

			// initially set importance to account balance
			importanceVector.setAt(i, account.getBalance().getNumNem());

			if (!accountInfo.hasOutLinks())
				dangleIndexes.add(i);

			++i;
		}

		// normalize the importance vector
		importanceVector.normalize();

		this.teleportationVector = createTeleportationVector();
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
	 * Gets the dangle indexes.
	 *
	 * @return The dangle indexes.
	 */
	public List<Integer> getDangleIndexes() {
		return this.dangleIndexes;
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

//	//Prepare outlink weights
//	double[][] outlinkWeights = new double[numAccounts][];
//	for (int ndx = 0; ndx < numAccounts; ndx++) {
//
//	 List<AccountLink> outlinks = accounts.get(ndx).getOutlinks();
//	 if (outlinks == null || outlinks.size() < 1) {
//	 continue;
//	 }
//
//	 double[] weights = getRightStochasticWeights(outlinks);
//	 outlinkWeights[ndx] = weights;
//	}
//
//	// normalize starting vector to sum to 1
//	importances.normalize();
//
//	double maxImportance = importances.max();
//
//	// calculate teleportation probabilities based on normalized amount of NEM owned
//	double[] teleporations = new double[numAccounts];
//	for (int acctNdx = 0; acctNdx < importances.getSize(); acctNdx++) {
//	 // Assign a value between .7 and .95 based on the amount of NEM in an account.
//	 // It seems that more NEM = higher teleportation seems to work better.
//	 // NOTE: importances were initialized with acct balances, so this is why this works;
//	 // also, the importance vector was already normalized to sum to 1 in the code above.
//	 teleporations[acctNdx] = MIN_TELEPORTATION_PROB
//	 + ADDITIVE_TELEPORTATION*(importances.getAt(acctNdx)/maxImportance);
//	}

}
