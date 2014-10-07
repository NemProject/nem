package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.FormatUtils;
import org.nem.nis.poi.graph.*;
import org.nem.nis.secret.AccountLink;
import org.nem.nis.test.*;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * If someone can manipulate their importance so that they can often or at-will
 * be chosen to forage, then things like double-spend attacks become possible.
 * Thus the tests considered here focus on verifying that a user cannot
 * arbitrarily manipulate their importance to cause them to be chosen to forage.
 * some tests we should consider: - Sybil attack (master node creates a ton of
 * other nodes and transacts with them (and maybe some other nodes) to try to
 * boost score)</br>
 * - infinite loop attack<br/>
 * - closed loop attack<br/>
 * - small transaction spam attack<br/>
 * -
 */
public class PoiImportanceCalculatorITCase {
	private static final Logger LOGGER = Logger.getLogger(PoiImportanceCalculatorITCase.class.getName());

	private static final PoiAccountState GENERAL_RECEIVER = createAccountWithBalance(10);

	private static final int OUTLINK_STRATEGY_NONE = 0;
	private static final int OUTLINK_STRATEGY_RANDOM = 1;
	private static final int OUTLINK_STRATEGY_LOOP = 2;
	private static final int OUTLINK_STRATEGY_LOOP_SELF = 3;
	private static final int OUTLINK_STRATEGY_ALL_TO_ONE = 4;
	private static final int OUTLINK_STRATEGY_TO_GENERAL_RECEIVER = 5;

	private static final double HIGH_TOLERANCE = 0.1;
	private static final double LOW_TOLERANCE = 0.05;

	//region clustering tests

	@Test
	public void fastScanClusteringResultsInSameImportancesAsScan() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(new ScanClusteringStrategy(), true);

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance < 0.001);
	}

	@Test
	public void fastScanClusteringResultsInDifferentImportancesThanOutlierScan() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(new OutlierScan(), true);

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance > 0.001);
	}

	@Test
	public void fastScanClusteringResultsInDifferentImportancesThanSingleClusterScan() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(new SingleClusterScan(), true);

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance > 0.001);
	}

	@Test
	public void fastScanClusteringResultsInDifferentImportancesThanNoClustering() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(new SingleClusterScan(), false);

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance > 0.001);
	}

	@Test
	public void fastScanClusteringCorrectlyIdentifiesMostImportantAccount() {
		// Act:
		final ColumnVector importances = calculateImportances(new FastScanClusteringStrategy(), true);
		int maxIndex = 0;
		int nextMaxIndex = 0;
		for (int i = 0; i < importances.size(); ++i) {
			if (importances.getAt(i) > importances.getAt(maxIndex)) {
				nextMaxIndex = maxIndex;
				maxIndex = i;
			} else if (importances.getAt(i) > importances.getAt(nextMaxIndex)) {
				nextMaxIndex = i;
			}
		}

		// Assert: max index and next max index are 4 and 5
		Assert.assertThat(maxIndex + nextMaxIndex, IsEqual.equalTo(9));
		Assert.assertThat(Math.abs(maxIndex - nextMaxIndex), IsEqual.equalTo(1));
	}

	private static double calculateDistanceBetweenFastScanAndOtherImportances(
			final GraphClusteringStrategy clusteringStrategy,
			final boolean useClusteringStrategy) {
		// Act:
		final ColumnVector fastScanImportances = calculateImportances(new FastScanClusteringStrategy(), true);
		final ColumnVector otherImportances = calculateImportances(clusteringStrategy, useClusteringStrategy);

		// Assert:
		final double distance = fastScanImportances.l2Distance(otherImportances);
		LOGGER.info(String.format("fastScanImportances - %s", fastScanImportances));
		LOGGER.info(String.format("otherImportances ---- %s", otherImportances));
		LOGGER.info(String.format("distance - %f", distance));
		return distance;

	}

	private static ColumnVector calculateImportances(final GraphClusteringStrategy clusteringStrategy, final boolean useClusteringStrategy) {
		final Collection<PoiAccountState> accountStates = createAccountStatesFromGraph(GraphType.GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS);

		final BlockHeight importanceBlockHeight = new BlockHeight(2);
		final ImportanceCalculator importanceCalculator = new PoiImportanceCalculator(new PoiScorer(), clusteringStrategy, useClusteringStrategy);
		importanceCalculator.recalculate(importanceBlockHeight, accountStates);
		final List<Double> importances = accountStates.stream()
				.map(a -> a.getImportanceInfo().getImportance(importanceBlockHeight))
				.collect(Collectors.toList());

		final ColumnVector importancesVector = new ColumnVector(importances.size());
		for (int i = 0; i < importances.size(); ++i) {
			importancesVector.setAt(i, importances.get(i));
		}

		return importancesVector;
	}

	private static Collection<PoiAccountState> createAccountStatesFromGraph(final GraphType graphType) {
		final Matrix outlinkMatrix = OutlinkMatrixFactory.create(graphType);

		final List<PoiAccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < outlinkMatrix.getRowCount(); ++i) {
			final PoiAccountState accountState = new PoiAccountState(Utils.generateRandomAddress());
			accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, Amount.fromNem(1000000000));
			accountStates.add(accountState);
		}

		final BlockHeight blockHeight = new BlockHeight(2);
		for (int i = 0; i < outlinkMatrix.getRowCount(); ++i) {
			final MatrixNonZeroElementRowIterator iterator = outlinkMatrix.getNonZeroElementRowIterator(i);
			while (iterator.hasNext()) {
				final MatrixElement element = iterator.next();
				final Amount amount = Amount.fromNem(element.getValue().longValue());
				if (amount.compareTo(Amount.ZERO) > 0) {
					final PoiAccountState senderAccountState = accountStates.get(element.getColumn());
					final PoiAccountState recipientAccountState = accountStates.get(element.getRow());
					senderAccountState.getWeightedBalances().addSend(blockHeight, amount);
					senderAccountState.getImportanceInfo().addOutlink(
							new AccountLink(blockHeight, amount, recipientAccountState.getAddress()));

					recipientAccountState.getWeightedBalances().addReceive(blockHeight, amount);
				}
			}
		}

		return accountStates;
	}

	final void printDifferences(final ColumnVector lhs, final ColumnVector rhs) {
		Assert.assertThat(lhs.size(), IsEqual.equalTo(rhs.size()));
		final ColumnVector ratios = new ColumnVector(lhs.size());
		double diff = 0;
		for (int i = 0; i < rhs.size(); ++i) {
			diff += Math.abs(rhs.getAt(i) - lhs.getAt(i));
			if (lhs.getAt(i) > 0.0) {
				ratios.setAt(i, rhs.getAt(i) / lhs.getAt(i));
			} else if (rhs.getAt(i) > 0.0) {
				ratios.setAt(i, Double.MAX_VALUE);
			} else {
				ratios.setAt(i, 1.0);
			}
			if (ratios.getAt(i) > 1.001 || ratios.getAt(i) < 0.999) {
				System.out.println("Account " + i + " importance ratio is " + ratios.getAt(i));
			}
		}

		System.out.println(lhs);
		System.out.println(rhs);
		System.out.println("ratios: " + ratios);
		System.out.println("diff: " + diff);
	}


	//endregion

	@Test
	public void threeSimpleAccounts() {
		// Arrange:
		final PoiAccountState a = createAccountWithBalance(100000);
		final PoiAccountState b = createAccountWithBalance(100000);
		final PoiAccountState c = createAccountWithBalance(100000);

		final BlockHeight blockHeight = new BlockHeight(10000);

		// A sends all 100000 NEM to B,
		this.addOutlink(a, b, new BlockHeight(1), 100000);

		final List<PoiAccountState> accts = Arrays.asList(a, b, c);

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(blockHeight, accts);
		System.out.println(importances);

		// Assert
		//a > b > c
		Assert.assertTrue(importances.getAt(0) > importances.getAt(1) && importances.getAt(1) > importances.getAt(2));
	}

	/**
	 * Four nodes (A, B, C, D) are owned by one person with 400 NEM who
	 * distributed the NEM
	 * between the nodes and cycled the NEM around. The other three nodes are
	 * independent and have 400 NEM each.
	 * The following transactions occur (transaction fees are assumed to be 0):
	 * A, E, F, G all start with 400 NEM; ABCD are all controlled by actor A.
	 * A sends all 400 NEM to B, who sends 300 NEM to C, who sends 200 NEM to D,
	 * who sends 100 to A.
	 * E starts with 400 NEM and sends 100 to G.
	 * G starts with 400 NEM, gets 100 from E, and sends 100 to F.
	 */
	@Test
	public void fourNodeSimpleLoopAttack() {

		// Arrange:
		final PoiAccountState a = createAccountWithBalance(400000);
		final PoiAccountState b = createAccountWithBalance(0);
		final PoiAccountState c = createAccountWithBalance(0);
		final PoiAccountState d = createAccountWithBalance(0);

		final PoiAccountState e = createAccountWithBalance(400000);
		final PoiAccountState f = createAccountWithBalance(400000);
		final PoiAccountState g = createAccountWithBalance(400000);

		final BlockHeight blockHeight = new BlockHeight(1);

		// A sends all 400000 NEM to B,
		this.addVestedOutlink(a, b, blockHeight, 400000);

		//who sends 300000 NEM to C,
		this.addVestedOutlink(b, c, blockHeight, 300000);

		//who sends 200000 NEM to D,
		this.addVestedOutlink(c, d, blockHeight, 200000);

		// who sends 100000 to A.
		this.addVestedOutlink(d, a, blockHeight, 100000);

		// e sends 100000 NEM to g
		this.addVestedOutlink(e, g, blockHeight, 100000);

		// g sends 100000 NEM to f
		this.addVestedOutlink(g, f, blockHeight, 100000);

		final List<PoiAccountState> accountStates = Arrays.asList(a, b, c, d, e, f, g);

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(2), accountStates);
		System.out.println(importances);

		// Assert:
		// E > G > F
		// E > A > others
		Assert.assertTrue(importances.getAt(4) > importances.getAt(6));// e>g
		Assert.assertTrue(importances.getAt(6) > importances.getAt(5));// g>f
		Assert.assertTrue(importances.getAt(4) > importances.getAt(0));// e>a
		Assert.assertTrue(importances.getAt(0) > importances.getAt(1));// a>b
		Assert.assertTrue(importances.getAt(0) > importances.getAt(2));// a>c
		Assert.assertTrue(importances.getAt(0) > importances.getAt(3));// a>d
	}

	private void addOutlink(final PoiAccountState a, final PoiAccountState b, final BlockHeight blockHeight, final long amount) {
		a.getImportanceInfo().addOutlink(new AccountLink(blockHeight, Amount.fromNem(amount), b.getAddress()));
	}

	private void addVestedOutlink(final PoiAccountState a, final PoiAccountState b, final BlockHeight blockHeight, final long amount) {
		a.getImportanceInfo().addOutlink(new AccountLink(blockHeight, Amount.fromNem(amount), b.getAddress()));
		b.getWeightedBalances().addFullyVested(blockHeight, Amount.fromNem(amount));
	}

	@Test
	public void twoAccountsLoopSelfVersusTwoAccountsLoop() {
		LOGGER.info("Self loop vs. normal loop");

		// Arrange:
		// TODO: Loops should be detected.
		final List<PoiAccountState> accountStates = new ArrayList<>();
		accountStates.addAll(this.createUserAccounts(1, 2, 1000000, 1, 500, OUTLINK_STRATEGY_LOOP_SELF));
		accountStates.addAll(this.createUserAccounts(1, 2, 1000000, 1, 500, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(10000), accountStates);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final double user1Importance = importances.getAt(1) + importances.getAt(2);
		final double user2Importance = importances.getAt(3) + importances.getAt(4);
		final double ratio = user1Importance / user2Importance;
		System.out.print("Self loop vs. normal loop: User 1 importance is " + format.format(user1Importance));
		System.out.print(", User 2 cumulative importance is " + format.format(user2Importance));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("");

		// Assert
		assertRatioIsWithinTolerance(ratio, LOW_TOLERANCE);
	}

	@Test
	public void accountSplittingDoesNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account vs. many accounts");

		// Arrange 1 vs many:
		// Splitting of one account into many small accounts should have no influence on the importance distribution.
		// TODO-CR 20140916 BR: test fails because weight on page rank was increased from 5% to 13.37%.
		final List<PoiAccountState> accounts = new ArrayList<>();
		for (int i = 2; i < 10; i++) {
			accounts.clear();
			accounts.add(GENERAL_RECEIVER);
			accounts.addAll(this.createUserAccounts(1, 1, 80000, 1, 40000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, i, 80000, 1, 40000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			final double cumulativeImportanceOtherAccounts = importances.sum() - importances.getAt(0) - importances.getAt(1);
			final double ratio = importances.getAt(1) / cumulativeImportanceOtherAccounts;
			System.out.print("1 vs. " + i + ": User 1 importance is " + format.format(importances.getAt(1)));
			System.out.print(", User 2 cumulative importance is " + cumulativeImportanceOtherAccounts);
			System.out.println(", ratio is " + format.format(ratio));

			// Assert
			assertRatioIsWithinTolerance(ratio, HIGH_TOLERANCE);
		}
		System.out.println("");
	}

	@Test
	public void manySmallLazyAcountsDoNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account vs. 8 accounts with many lazy accounts");

		// Arrange 1 vs 8, with lazy accounts:
		// The presence of many small lazy accounts should have no influence on the importance distribution.
		final List<PoiAccountState> accounts = new ArrayList<>();
		for (int i = 40; i < 400; i = i + 40) {
			accounts.clear();
			accounts.add(GENERAL_RECEIVER);
			accounts.addAll(this.createUserAccounts(1, 1, 8000000, 1, 400000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, 8, 8000000, 1, 400000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, i, i * 5000, 0, 0, OUTLINK_STRATEGY_NONE));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			double user2Importance = 0;
			for (int j = 2; j < 10; j++) {
				user2Importance += importances.getAt(j);
			}
			final double ratio = importances.getAt(1) / user2Importance;
			System.out.print("1 vs. 8 with " + i + " small lazy accounts: User 1 importance is " + format.format(importances.getAt(1)));
			System.out.print(", User 2 cumulative importance is " + format.format(user2Importance));
			System.out.println(", ratio is " + format.format(ratio));

			// Assert
			assertRatioIsWithinTolerance(ratio, HIGH_TOLERANCE);
		}
		System.out.println("");
	}

	@Test
	public void oneBigLazyAcountDoesNotInfluencesImportanceDistribution() {
		LOGGER.info("1 account vs. 8 accounts with 0 or 1 big lazy account");

		// Arrange 1 vs 8, with 0 or 1 big lazy account:
		// The presence of a big lazy account should have no influence on the relative importance distribution.
		// TODO-CR 20140916 BR: test fails because the ratio of balance+outlink weight to page rank weight is about 1:1 for the small accounts.
		final List<PoiAccountState> accounts = new ArrayList<>();
		for (int i = 1; i < 2; i++) {
			accounts.clear();
			accounts.add(GENERAL_RECEIVER);
			accounts.addAll(this.createUserAccounts(1, 1, 80000, 1, 40000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, 8, 80000, 1, 40000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, i, i * 8000000, 0, 0, OUTLINK_STRATEGY_NONE));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			double user2Importance = 0;
			for (int j = 2; j < 10; j++) {
				user2Importance += importances.getAt(j);
			}
			final double ratio = importances.getAt(1) / user2Importance;
			System.out.print("1 vs. 8 with " + i + " big lazy account: User 1 importance is " + format.format(importances.getAt(1)));
			System.out.print(", User 2 cumulative importance is " + format.format(user2Importance));
			System.out.println(", ratio is " + format.format(ratio));

			// Assert
			assertRatioIsWithinTolerance(ratio, HIGH_TOLERANCE);
		}
		System.out.println("");
	}

	@Test
	public void outlinkStrengthSplittingDoesNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account with 1 outlink vs. 1 account many outlinks (same cumulative strength)");

		// Arrange: 1 vs 1, the latter distributes the strength to many outlinks:
		// Splitting one transaction into many small transactions should have no influence on the importance distribution.
		final List<PoiAccountState> accounts = new ArrayList<>();
		for (int i = 1; i < 10; i++) {
			accounts.clear();
			accounts.add(GENERAL_RECEIVER);
			accounts.addAll(this.createUserAccounts(1, 1, 8000, 1, 400, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, 1, 8000, i, 400, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			final double ratio = importances.getAt(1) / importances.getAt(2);
			System.out.print("1 outlink vs. " + i + " outlinks: User 1 importance is " + format.format(importances.getAt(1)));
			System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(2)));
			System.out.println(", ratio is " + format.format(ratio));

			// Assert
			assertRatioIsWithinTolerance(ratio, LOW_TOLERANCE);
		}
		System.out.println("");
	}

	@Test
	public void outlinkStrengthDoesInfluenceImportanceDistribution() {
		LOGGER.info("High outlink strength vs. low outlink strength");

		// Arrange:
		// The strength of an outlink should have influence on the importance distribution (but how much?).
		final List<PoiAccountState> accounts = new ArrayList<>();
		accounts.add(GENERAL_RECEIVER);
		accounts.addAll(this.createUserAccounts(1, 2, 100000, 1, 50000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
		accounts.addAll(this.createUserAccounts(1, 2, 100000, 1, 5000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final double user1Importance = importances.getAt(1) + importances.getAt(2);
		final double user2Importance = importances.getAt(3) + importances.getAt(4);
		final double ratio = user1Importance / user2Importance;
		System.out.print("High outlink strength vs. low outlink strength: User 1 importance is " + format.format(user1Importance));
		System.out.print(", User 2 cumulative importance is " + format.format(user2Importance));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("");

		// Assert
		Assert.assertTrue(ratio > 1.0);
	}

	@Test
	public void vestedBalanceDoesInfluenceImportanceDistribution() {
		LOGGER.info("High balance vs. low balance");

		// Arrange:
		// The vested balance of an account should have influence on the importance distribution.
		final List<PoiAccountState> accounts = new ArrayList<>();
		accounts.add(GENERAL_RECEIVER);
		accounts.addAll(this.createUserAccounts(1, 2, 1000000, 1, 5000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
		accounts.addAll(this.createUserAccounts(1, 2, 10000, 1, 5000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final double user1Importance = importances.getAt(1) + importances.getAt(2);
		final double user2Importance = importances.getAt(3) + importances.getAt(4);
		final double ratio = user1Importance / user2Importance;
		System.out.print("High balance vs. low balance: User 1 importance is " + format.format(user1Importance));
		System.out.print(", User 2 cumulative importance is " + format.format(user2Importance));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("");

		// Assert
		Assert.assertTrue(ratio > 10.0);
	}

	@Test
	public void poiIsFairerThanPOS() {
		LOGGER.info("Check that POI distributes importance differently than POS");
		// Test should verify that accts that transact can gain higher importance than richer accts that do not.
		// Arrange:
		// Accounts with smaller vested balance should be able to have more importance than accounts with high balance and low activity
		final int numAccounts = 10;

		final List<PoiAccountState> accounts = new ArrayList<>();
		accounts.add(GENERAL_RECEIVER);
		accounts.addAll(this.createUserAccounts(1, numAccounts, 110000, 1, 1, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
		accounts.addAll(this.createUserAccounts(1, numAccounts, 100000, 1, 50000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();

		double highBalanceSum = 0;
		double lowBalanceSum = 0;

		for (int ndx = 1; ndx <= numAccounts; ndx++) {
			highBalanceSum += importances.getAt(ndx);
			lowBalanceSum += importances.getAt(ndx + numAccounts);
		}

		final double ratio = highBalanceSum / lowBalanceSum;
		System.out.print("High balance vs. low balance: User 1 importance is " + format.format(highBalanceSum));
		System.out.print(", User 2 cumulative importance is " + format.format(lowBalanceSum));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("Importances: " + importances);
		System.out.println("");

		// Assert
		Assert.assertTrue(ratio < 1.0);
	}

	@Test
	public void accountCannotBoostPOIWithVeryLowBalance() {
		LOGGER.info("Check that an account can't just send most of their balance to another account to boost their score");
		// Arrange:
		// Accounts should not just be able to transfer all their balance to another account to boost their score
		final List<PoiAccountState> accounts = new ArrayList<>();
		accounts.add(GENERAL_RECEIVER);
		accounts.addAll(this.createUserAccounts(1, 2, 20000, 2, 100, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
		accounts.addAll(this.createUserAccounts(1, 2, 20000, 2, 9900, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final double user1Importance = importances.getAt(1) + importances.getAt(2);
		final double user2Importance = importances.getAt(3) + importances.getAt(4);
		final double ratio = user1Importance / user2Importance;
		System.out.print("High balance vs. low balance: User 1 importance is " + format.format(user1Importance));
		System.out.print(", User 2 cumulative importance is " + format.format(user2Importance));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("Importances: " + importances);
		System.out.println("");

		// Assert
		Assert.assertTrue(ratio > .90);
	}

	@Test
	public void pushOneAccountDoesNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account with 1 outlink vs. many accounts with outlinks to one account (same cumulative strength)");

		// Arrange 1 vs many, the latter concentrate the strength to one account:
		// Colluding accounts that try to push one account with many links should have no influence on the importance distribution.
		final List<PoiAccountState> accounts = new ArrayList<>();
		for (int i = 4; i < 40; i++) {
			accounts.clear();
			accounts.add(GENERAL_RECEIVER);
			accounts.addAll(this.createUserAccounts(1, 1, 8000, 1, 400, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, i, 8000, 1, 400, OUTLINK_STRATEGY_ALL_TO_ONE));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			//			System.out.println("importances: " + importances);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			final double cumulativeImportanceOtherAccounts = importances.sum() - importances.getAt(0) - importances.getAt(1);
			final double ratio = importances.getAt(1) / cumulativeImportanceOtherAccounts;
			System.out.print("1 vs. " + i + ", outlink directed to one account: User 1 importance is " + format.format(importances.getAt(1)));
			System.out.print(", User 2 cumulative importance is " + format.format(cumulativeImportanceOtherAccounts));
			System.out.println(", ratio is " + format.format(ratio));

			// Assert
			assertRatioIsWithinTolerance(ratio, HIGH_TOLERANCE);
		}
		System.out.println("");
	}

	@Test
	public void poiCalculationIsPerformantEnough() {
		LOGGER.info("Testing performance of the poi calculation");

		// Arrange:
		// The poi calculation should take no more than a second even for MANY accounts (~ million)
		// TODO: why 1s?
		// BR: 1s is probably way to high. We will need to address this later.
		System.out.println("Setting up accounts.");
		final int numAccounts = 50000;
		final List<PoiAccountState> accounts = new ArrayList<>();
		accounts.addAll(this.createUserAccounts(1, numAccounts, 10000 * numAccounts, 2, 500 * numAccounts, OUTLINK_STRATEGY_RANDOM));

		// TODO 20140929 BR: Why is everything so damn slow in the first round?
		// TODO 20141003 M-BR: lazy class loading, real-time optimization, and JIT compilation: http://stackoverflow.com/questions/1481853/technique-or-utility-to-minimize-java-warm-up-time
		// Warm up phase
		getAccountImportances(new BlockHeight(10000), accounts);

		// Act: calculate importances
		System.out.println("Starting poi calculation.");
		final long start = System.currentTimeMillis();
		for (int i = 0; i < 5; i++) {
			final ColumnVector importances = getAccountImportances(new BlockHeight(10000), accounts);
		}
		final long stop = System.currentTimeMillis();
		System.out.println("Finished poi calculation.");

		System.out.println("For " + numAccounts + " accounts the poi calculation needed " + (stop - start) / 5 + "ms.");

		// Assert
		Assert.assertTrue(stop - start < 1000);
	}

	/**
	 * Test to see if the calculation time grows approximately linearly with the
	 * input.
	 */
	@Test
	public void poiCalculationHasLinearPerformance() {
		LOGGER.info("Testing linear performance of the poi calculation");

		// The poi calculation should take no more than a second even for MANY accounts (~ million)

		final BlockHeight height = new BlockHeight(10000);
		long prevTimeDiff = -1;
		for (int numAccounts = 5; numAccounts < 10000; numAccounts *= 10) {
			// Arrange:
			final List<PoiAccountState> accounts = new ArrayList<>();
			accounts.addAll(this.createUserAccounts(1, numAccounts, 10000000, 1, 500, OUTLINK_STRATEGY_LOOP));

			// Act: calculate importances
			System.out.println("Starting poi calculation.");
			final long start = System.currentTimeMillis();
			getAccountImportances(height, accounts);
			final long stop = System.currentTimeMillis();
			System.out.println("Finished poi calculation.");

			System.out.println("For " + numAccounts + " accounts the poi calculation needed " + (stop - start) + "ms.");

			// Assert
			final long currTimeDiff = stop - start;

			if (prevTimeDiff > 0) {
				final double ratio = prevTimeDiff * 10. / currTimeDiff;
				System.out.println("Prev time: " + prevTimeDiff
						+ "\tCurr Time:" + currTimeDiff + "\tRatio: " + ratio);

				Assert.assertTrue(ratio > .9);
			}

			prevTimeDiff = currTimeDiff;
		}
	}

	private List<PoiAccountState> createUserAccounts(
			final long blockHeight,
			final int numAccounts,
			final long totalVestedBalance,
			final int numOutlinksPerAccount,
			final long totalOutlinkStrength,
			final int outlinkStrategy) {
		final List<PoiAccountState> accounts = new ArrayList<>();

		for (int i = 0; i < numAccounts; i++) {
			if (outlinkStrategy == OUTLINK_STRATEGY_ALL_TO_ONE) {
				if (i == 0) {
					accounts.add(createAccountWithBalance(String.valueOf(i), blockHeight, totalVestedBalance - totalOutlinkStrength - numAccounts + 1));
				} else {
					accounts.add(createAccountWithBalance(String.valueOf(i), blockHeight, 1));
				}
			} else {
				accounts.add(createAccountWithBalance(String.valueOf(i), blockHeight, (totalVestedBalance - totalOutlinkStrength) / numAccounts));
			}
		}

		final SecureRandom sr = new SecureRandom();
		PoiAccountState otherAccount = null;
		for (int i = 0; i < numAccounts; ++i) {
			final PoiAccountState account = accounts.get(i);
			for (int j = 0; j < numOutlinksPerAccount; ++j) {
				switch (outlinkStrategy) {
					case OUTLINK_STRATEGY_RANDOM:
						otherAccount = accounts.get(sr.nextInt(numAccounts));
						break;
					case OUTLINK_STRATEGY_LOOP:
						otherAccount = accounts.get((i + 1) % numAccounts);
						break;
					case OUTLINK_STRATEGY_LOOP_SELF:
						otherAccount = account;
						break;
					case OUTLINK_STRATEGY_ALL_TO_ONE:
						otherAccount = accounts.get(0);
						break;
					case OUTLINK_STRATEGY_TO_GENERAL_RECEIVER:
						otherAccount = GENERAL_RECEIVER;
						break;
				}

				final Amount vested = account.getWeightedBalances().getVested(new BlockHeight(blockHeight));
				final long outlinkStrength =
						(vested.getNumNem() * totalOutlinkStrength) / ((totalVestedBalance - totalOutlinkStrength) * numOutlinksPerAccount);
				this.addOutlink(account, otherAccount, BlockHeight.ONE, outlinkStrength);
			}
		}

		return accounts;
	}

	private static void assertRatioIsWithinTolerance(final double ratio, final double tolerance) {
		Assert.assertTrue(1.0 - tolerance < ratio && ratio < 1.0 + tolerance);
	}

	private static PoiAccountState createAccountWithBalance(final long numNEM) {
		return createAccountWithBalance(Utils.generateRandomAddress().getEncoded(), 1, numNEM);
	}

	private static PoiAccountState createAccountWithBalance(final String id, final long blockHeight, final long numNEM) {
		final PoiAccountState state = new PoiAccountState(Address.fromEncoded("account" + id));
		state.getWeightedBalances().addFullyVested(new BlockHeight(blockHeight), Amount.fromNem(numNEM));
		return state;
	}

	private static ColumnVector getAccountImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accounts) {
		final ImportanceCalculator importanceCalculator = NisUtils.createImportanceCalculator();
		importanceCalculator.recalculate(blockHeight, accounts);
		final List<Double> importances = accounts.stream()
				.map(a -> a.getImportanceInfo().getImportance(blockHeight))
				.collect(Collectors.toList());

		final ColumnVector importancesVector = new ColumnVector(importances.size());
		for (int i = 0; i < importances.size(); ++i) {
			importancesVector.setAt(i, importances.get(i));
		}

		return importancesVector;
	}
}
