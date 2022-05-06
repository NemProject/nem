package org.nem.nis.pox.poi;

import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.FormatUtils;
import org.nem.nis.pox.ImportanceCalculator;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

import java.lang.management.*;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * If someone can manipulate their importance so that they can often or at-will
 * be chosen to harvest, then things like double-spend attacks become possible.
 * Thus the tests considered here focus on verifying that a user cannot
 * arbitrarily manipulate their importance to cause them to be chosen to harvest.
 * <br>
 * Some tests we consider:
 * - Sybil attack (master node creates a ton of
 * other nodes and transacts with them (and maybe some other nodes) to try to
 * boost score)
 * - infinite loop attack (sending XEM around in a loop to boost their score)
 */
public class PoiImportanceCalculatorITCase {
	private static final Logger LOGGER = Logger.getLogger(PoiImportanceCalculatorITCase.class.getName());

	private static final AccountState GENERAL_RECEIVER = createAccountWithBalance(10);

	private static final int OUTLINK_STRATEGY_NONE = 0;
	private static final int OUTLINK_STRATEGY_RANDOM = 1;
	private static final int OUTLINK_STRATEGY_LOOP = 2;
	private static final int OUTLINK_STRATEGY_LOOP_SELF = 3;
	private static final int OUTLINK_STRATEGY_ALL_TO_ONE = 4;
	private static final int OUTLINK_STRATEGY_TO_GENERAL_RECEIVER = 5;

	private static final double SUPER_HIGH_TOLERANCE = 0.2;
	private static final double HIGH_TOLERANCE = 0.1;
	private static final double LOW_TOLERANCE = 0.05;

	private static final long VESTED_8M = 8_000_000;
	private static final long VESTED_80M = VESTED_8M * 10;
	private static final long VESTED_800M = VESTED_80M * 10;

	private static final long TOTAL_OUTLINK_40K = 40_000;
	private static final long TOTAL_OUTLINK_400K = TOTAL_OUTLINK_40K * 10;
	private static final long TOTAL_OUTLINK_40M = TOTAL_OUTLINK_400K * 100;

	/**
	 * Four nodes (A, B, C, D) are owned by one person with 400000 NEM who
	 * distributed the NEM
	 * between the nodes and cycled the NEM around. The other three nodes are
	 * independent and have 400000 NEM each.
	 * The following transactions occur (transaction fees are assumed to be 0):
	 * A, E, F, G all start with 400000 NEM; ABCD are all controlled by actor A.
	 * A sends all 400 NEM to B, who sends 300000 NEM to C, who sends 200000 NEM to D,
	 * who sends 100000 to A.
	 * E starts with 400000 NEM and sends 100000 to G.
	 * G starts with 400000 NEM, gets 100000 from E, and sends 100000 to F.
	 */
	@Test
	public void fourNodeSimpleLoopAttack() {

		// Arrange:
		final AccountState a = createAccountWithBalance(400000);
		final AccountState b = createAccountWithBalance(0);
		final AccountState c = createAccountWithBalance(0);
		final AccountState d = createAccountWithBalance(0);

		final AccountState e = createAccountWithBalance(400000);
		final AccountState f = createAccountWithBalance(400000);
		final AccountState g = createAccountWithBalance(400000);

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

		final List<AccountState> accountStates = Arrays.asList(a, b, c, d, e, f, g);

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(2), accountStates);
		LOGGER.info(importances.toString());

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

	private void addOutlink(final AccountState a, final AccountState b, final BlockHeight blockHeight, final long amount) {
		a.getImportanceInfo().addOutlink(new AccountLink(blockHeight, Amount.fromNem(amount), b.getAddress()));
	}

	private void addVestedOutlink(final AccountState a, final AccountState b, final BlockHeight blockHeight, final long amount) {
		a.getImportanceInfo().addOutlink(new AccountLink(blockHeight, Amount.fromNem(amount), b.getAddress()));
		b.getWeightedBalances().addFullyVested(blockHeight, Amount.fromNem(amount));
	}

	@Test
	public void twoAccountsLoopSelfVersusTwoAccountsLoop() {
		LOGGER.info("Self loop vs. normal loop");

		// Arrange:
		final List<AccountState> accountStates = new ArrayList<>();
		accountStates.addAll(this.createUserAccounts(1, 2, 1000000, 1, 500, OUTLINK_STRATEGY_LOOP_SELF));
		accountStates.addAll(this.createUserAccounts(1, 2, 1000000, 1, 500, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(10000), accountStates);

		final double user1Importance = importances.getAt(0) + importances.getAt(1);
		final double user2Importance = importances.getAt(2) + importances.getAt(3);
		final double ratio = user1Importance / user2Importance;
		outputComparison("Self loop vs. normal loop", user1Importance, user2Importance);

		// Assert
		assertRatioIsWithinTolerance(ratio, LOW_TOLERANCE);
	}

	@Test
	public void accountSplittingDoesNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account vs. many accounts");

		// Arrange 1 vs many:
		// Splitting of one account into many small accounts should have no influence on the importance distribution.
		final List<AccountState> accounts = new ArrayList<>();
		for (int i = 2; i < 30; ++i) {
			accounts.clear();
			accounts.add(GENERAL_RECEIVER);
			accounts.addAll(this.createUserAccounts(1, 1, VESTED_80M, 1, TOTAL_OUTLINK_40M, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, i, VESTED_80M, 1, TOTAL_OUTLINK_40M, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final double cumulativeImportanceOtherAccounts = importances.sum() - importances.getAt(0) - importances.getAt(1);
			final double ratio = importances.getAt(1) / cumulativeImportanceOtherAccounts;
			outputComparison("1 vs. " + i, importances.getAt(1), cumulativeImportanceOtherAccounts);

			// Assert
			assertRatioIsWithinTolerance(ratio, SUPER_HIGH_TOLERANCE);
		}
	}

	@Test
	public void manySmallLazyAccountsDoNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account vs. 8 accounts with many lazy accounts");

		// Arrange 1 vs 8, with lazy accounts:
		// The presence of many small lazy accounts should have no influence on the importance distribution.
		final List<AccountState> accounts = new ArrayList<>();
		for (int i = 40; i < 400; i = i + 40) {
			accounts.clear();
			accounts.add(GENERAL_RECEIVER);
			accounts.addAll(this.createUserAccounts(1, 1, VESTED_80M, 1, TOTAL_OUTLINK_400K, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, 8, VESTED_80M, 1, TOTAL_OUTLINK_400K, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, i, i * 50000, 0, 0, OUTLINK_STRATEGY_NONE));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			double user2Importance = 0;
			for (int j = 2; j < 10; ++j) {
				user2Importance += importances.getAt(j);
			}
			final double ratio = importances.getAt(1) / user2Importance;
			outputComparison("1 vs. 8 with " + i + " small lazy accounts", importances.getAt(1), user2Importance);

			// Assert
			assertRatioIsWithinTolerance(ratio, HIGH_TOLERANCE);
		}
	}

	@Test
	public void oneBigLazyAccountInfluencesImportanceDistribution() {
		LOGGER.info("1 account vs. 8 accounts with 0 or 1 big lazy account");

		// Arrange 1 vs 8, with 0 or 1 big lazy account:
		// The presence of a big lazy account ideally should have no influence on the relative importance distribution.
		//
		// Note that this test fails for the following reason:
		// 1) each of the accounts has the same page rank of 0.1 (which is multiplied by 0.1337 to give 0.01337)
		// 2) without the huge account the outlink weight is dominating the importance and thus there is no big difference
		//    when splitting 1 account into 8 small accounts. But when the huge account is present the first account's outlink weight
		//    drops to only 0.01 which is about the same size as the page rank.
		// So in the latter case the overall importance for the first account is only about twice the importance of each of the 8 small accounts.
		// This could only be "fixed" by giving the page rank a lower weight which is not wanted.
		// The users are allowed to game the importance calculation up to a certain degree (limited by 13.37% influence of the page rank)
		final List<AccountState> accounts = new ArrayList<>();
		final double[] ratios = new double[2];
		for (int i = 0; i < 2; ++i) {
			accounts.clear();
			accounts.add(GENERAL_RECEIVER);
			accounts.addAll(this.createUserAccounts(1, 1, VESTED_8M, 1, TOTAL_OUTLINK_400K, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, 8, VESTED_8M, 1, TOTAL_OUTLINK_400K, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, i, i * VESTED_800M, 0, 0, OUTLINK_STRATEGY_NONE));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			double user2Importance = 0;
			for (int j = 2; j < 10; ++j) {
				user2Importance += importances.getAt(j);
			}
			ratios[i] = importances.getAt(1) / user2Importance;
			outputComparison("1 vs. 8 with " + i + " big lazy account", importances.getAt(1), user2Importance);
		}

		// Assert
		LOGGER.info(String.format("The ratio changed from %.03f (without huge account) to %.03f (with huge account).", ratios[0], ratios[1]));
		Assert.assertTrue(ratios[0] > 0.75);
		Assert.assertTrue(ratios[1] < 0.25);
	}

	@Test
	public void outlinkStrengthSplittingDoesNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account with 1 outlink vs. 1 account many outlinks (same cumulative strength)");

		// Arrange: 1 vs 1, the latter distributes the strength to many outlinks:
		// Splitting one transaction into many small transactions should have no influence on the importance distribution.
		final List<AccountState> accounts = new ArrayList<>();
		for (int i = 1; i < 10; ++i) {
			accounts.clear();
			accounts.add(GENERAL_RECEIVER);
			accounts.addAll(this.createUserAccounts(1, 1, VESTED_8M, 1, TOTAL_OUTLINK_40K, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, 1, VESTED_8M, i, TOTAL_OUTLINK_40K, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final double ratio = importances.getAt(1) / importances.getAt(2);
			outputComparison("1 outlink vs. " + i + " outlinks", importances.getAt(1), importances.getAt(2));

			// Assert
			assertRatioIsWithinTolerance(ratio, LOW_TOLERANCE);
		}
	}

	@Test
	public void outlinkStrengthDoesInfluenceImportanceDistribution() {
		LOGGER.info("High outlink strength vs. low outlink strength");

		// Arrange:
		// The strength of an outlink should have influence on the importance distribution (but how much?).
		final List<AccountState> accounts = new ArrayList<>();
		accounts.add(GENERAL_RECEIVER);
		accounts.addAll(this.createUserAccounts(1, 2, 1000000, 1, 500000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
		accounts.addAll(this.createUserAccounts(1, 2, 1000000, 1, 50000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final double user1Importance = importances.getAt(1) + importances.getAt(2);
		final double user2Importance = importances.getAt(3) + importances.getAt(4);
		final double ratio = user1Importance / user2Importance;
		outputComparison("High outlink strength vs. low outlink strength", user1Importance, user2Importance);

		// Assert
		Assert.assertTrue(ratio > 1.0);
	}

	@Test
	public void vestedBalanceDoesInfluenceImportanceDistribution() {
		LOGGER.info("High balance vs. low balance");

		// Arrange:
		// The vested balance of an account should have influence on the importance distribution.
		final List<AccountState> accounts = new ArrayList<>();
		accounts.add(GENERAL_RECEIVER);
		accounts.addAll(this.createUserAccounts(1, 2, 1000000, 1, 5000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
		accounts.addAll(this.createUserAccounts(1, 2, 10000, 1, 5000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final double user1Importance = importances.getAt(1) + importances.getAt(2);
		final double user2Importance = importances.getAt(3) + importances.getAt(4);
		final double ratio = user1Importance / user2Importance;
		outputComparison("High balance vs. low balance", user1Importance, user2Importance);

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

		final List<AccountState> accounts = new ArrayList<>();
		accounts.add(GENERAL_RECEIVER);
		accounts.addAll(this.createUserAccounts(1, numAccounts, 110000000, 1, 1, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
		accounts.addAll(this.createUserAccounts(1, numAccounts, 100000000, 1, TOTAL_OUTLINK_40M, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		double highBalanceSum = 0;
		double lowBalanceSum = 0;

		for (int ndx = 1; ndx <= numAccounts; ++ndx) {
			highBalanceSum += importances.getAt(ndx);
			lowBalanceSum += importances.getAt(ndx + numAccounts);
		}

		final double ratio = highBalanceSum / lowBalanceSum;
		outputComparison("High balance vs. low balance", highBalanceSum, lowBalanceSum);
		LOGGER.info("Importances: " + importances);

		// Assert
		Assert.assertTrue(ratio < 1.0);
	}

	@Test
	public void accountCanSlightlyBoostPoiWithVeryLowBalanceByCreatingLargeOutlinks() {
		LOGGER.info("Check that an account can't just send most of their balance to another account to boost their score");
		// Arrange:
		// Accounts should not just be able to transfer all their balance to another account to boost their score
		final List<AccountState> accounts = new ArrayList<>();
		accounts.add(GENERAL_RECEIVER);
		accounts.addAll(this.createUserAccounts(1, 2, 2000000, 2, 10000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
		accounts.addAll(this.createUserAccounts(1, 2, 2000000, 2, 990000, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
		final double user1Importance = importances.getAt(1) + importances.getAt(2);
		final double user2Importance = importances.getAt(3) + importances.getAt(4);
		final double ratio = user1Importance / user2Importance;
		outputComparison("High balance vs. low balance", user1Importance, user2Importance);
		LOGGER.info("Importances: " + importances);

		// Assert: outlinks are given more weight than balance, so the low-balance account
		// should have boosted its poi by a small amount
		Assert.assertTrue(ratio > .90);
	}

	@Test
	public void pushOneAccountDoesNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account with 1 outlink vs. many accounts with outlinks to one account (same cumulative strength)");

		// Arrange 1 vs many, the latter concentrate the strength to one account:
		// Colluding accounts that try to push one account with many links should have no influence on the importance distribution.
		final List<AccountState> accounts = new ArrayList<>();
		for (int i = 4; i < 40; ++i) {
			accounts.clear();
			accounts.add(GENERAL_RECEIVER);
			accounts.addAll(this.createUserAccounts(1, 1, VESTED_8M, 1, TOTAL_OUTLINK_40K, OUTLINK_STRATEGY_TO_GENERAL_RECEIVER));
			accounts.addAll(this.createUserAccounts(1, i, VESTED_8M, 1, TOTAL_OUTLINK_40K, OUTLINK_STRATEGY_ALL_TO_ONE));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final double cumulativeImportanceOtherAccounts = importances.sum() - importances.getAt(0) - importances.getAt(1);
			final double ratio = importances.getAt(1) / cumulativeImportanceOtherAccounts;
			outputComparison("1 vs. " + i + ", outlink directed to one account", importances.getAt(1), cumulativeImportanceOtherAccounts);

			// Assert
			assertRatioIsWithinTolerance(ratio, HIGH_TOLERANCE);
		}
	}

	@Test
	public void poiCalculationIsFastEnough() {
		LOGGER.info("Testing performance of the poi calculation");

		// Arrange:
		// The poi calculation should take no more than a second even for MANY accounts (~ million)
		LOGGER.info("Setting up accounts.");
		final int numAccounts = 50000;
		final List<AccountState> accounts = new ArrayList<>();
		accounts.addAll(this.createUserAccounts(1, numAccounts, 50000L * numAccounts, 2, 500 * numAccounts, OUTLINK_STRATEGY_RANDOM));

		// Warm up phase
		for (int i = 0; i < 5; i++) {
			getAccountImportances(new BlockHeight(9990 + i), accounts);
		}

		// Act: calculate importances
		LOGGER.info("Starting poi calculation.");
		final int count = 5;
		final long start = System.currentTimeMillis();
		for (int i = 0; i < count; ++i) {
			getAccountImportances(new BlockHeight(10000 + i), accounts);
		}
		final long stop = System.currentTimeMillis();
		LOGGER.info("Finished poi calculation.");

		LOGGER.info("For " + numAccounts + " accounts the poi calculation needed on average " + (stop - start) / count + "ms.");

		// Assert
		// > context setup needs 646ms
		//   - AccountProcessor ctor needs 127ms
		//   - setup vested balance vector needs 24ms
		//   - add reverse links to allow net calculation needed 95ms
		//   - update the matrix with all net outflows needed 281ms
		//   - removeLessThan and normalizeColumns needed 12ms
		//   - buildInterLevelProximityMatrix needed 107ms
		// > clustering needs about 110ms.
		// > POI iterator needs 30ms.
		// So the context setup is the most expensive step.
		Assert.assertTrue((stop - start) / count < 2000);
	}

	/**
	 * Test to see if the calculation time is reasonably bounded as the number of accounts increases.
	 */
	@Test
	public void poiCalculationPerformanceGrowthIsReasonablyBoundedAsNumberOfAccountsIncreases() {
		LOGGER.info("Testing linear performance of the poi calculation");

		// The poi calculation should take no more than a second even for MANY accounts (~ million)

		final BlockHeight height = new BlockHeight(10000);
		long prevTimeDiff = -1;
		for (int numAccounts = 100; numAccounts < 10000; numAccounts *= 2) {
			// Arrange:
			final List<AccountState> accounts = new ArrayList<>();
			accounts.addAll(this.createUserAccounts(1, numAccounts, 100_000_000, 1, 500, OUTLINK_STRATEGY_LOOP));

			// Act: calculate importances
			LOGGER.info("Starting poi calculation.");
			final long start = System.currentTimeMillis();
			for (int i = 0; i < 100; ++i) {
				getAccountImportances(height, accounts);
			}
			final long stop = System.currentTimeMillis();
			LOGGER.info("Finished poi calculation.");

			LOGGER.info("For " + numAccounts + " accounts the poi calculation needed " + (stop - start) + "ms.");

			// Assert
			final long currTimeDiff = stop - start;

			if (prevTimeDiff > 0) {
				final double ratio = prevTimeDiff * 2. / currTimeDiff;
				LOGGER.info("Prev time: " + prevTimeDiff
						+ "\tCurr Time:" + currTimeDiff + "\tRatio: " + ratio);

				Assert.assertTrue(ratio > 0.75);
				Assert.assertTrue(currTimeDiff / 1000. < 1000);
			}

			prevTimeDiff = currTimeDiff;
		}
	}

	@Test
	public void poiCalculationHasModerateMemoryUsage() {
		LOGGER.info("Testing memory usage of the poi calculation");

		// Arrange:
		LOGGER.info("Setting up accounts.");
		final int numAccounts = 50000;
		final List<AccountState> accounts = new ArrayList<>();
		accounts.addAll(this.createUserAccounts(1, numAccounts, 50000l * numAccounts, 2, 500 * numAccounts, OUTLINK_STRATEGY_RANDOM));

		// Warm up phase
		getAccountImportances(new BlockHeight(9999), accounts);

		// Act: calculate importances
		LOGGER.info("Starting poi calculation.");
		final long start = System.currentTimeMillis();
		final long startHeapSize = Runtime.getRuntime().totalMemory();
		for (int i = 0; i < 5; ++i) {
			getAccountImportances(new BlockHeight(10000 + i), accounts);
		}

		final long endHeapSize = Runtime.getRuntime().totalMemory();

		final long stop = System.currentTimeMillis();
		LOGGER.info("Finished poi calculation.");

		LOGGER.info("For " + numAccounts + " accounts the poi calculation needed " + (stop - start) / 5 + "ms.");

		// Assert
		LOGGER.info("Heap: " + ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
		LOGGER.info("NonHeap: " + ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage());
		final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
		for (final MemoryPoolMXBean bean : beans) {
			LOGGER.info(bean.getName() + " : " + bean.getUsage());
			if ("PS Eden Space".equals(bean.getName())) {
				Assert.assertTrue(bean.getUsage().getUsed() < 256000000); // ~256 Mb
			} else if ("PS Survivor Space".equals(bean.getName())) {
				Assert.assertTrue(bean.getUsage().getUsed() < 256000000); // ~256 Mb
			} else if ("PS Old Gen".equals(bean.getName())) {
				Assert.assertTrue(bean.getUsage().getUsed() < 512000000); // ~512 Mb
			}
		}

		// Not so meaningful because the GC will affect this a lot
		final long heapSizeDiff = endHeapSize - startHeapSize;
		Assert.assertTrue(heapSizeDiff < 256000000); // ~256 Mb

		for (final GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			LOGGER.info(bean.getName() + " : " + bean.getCollectionCount() + " : " + bean.getCollectionTime());
		}
	}

	private List<AccountState> createUserAccounts(
			final long blockHeight,
			final int numAccounts,
			final long totalVestedBalance,
			final int numOutlinksPerAccount,
			final long totalOutlinkStrength,
			final int outlinkStrategy) {
		final List<AccountState> accounts = new ArrayList<>();

		for (int i = 0; i < numAccounts; ++i) {
			if (outlinkStrategy == OUTLINK_STRATEGY_ALL_TO_ONE) {
				if (0 == i) {
					accounts.add(createAccountWithBalance(String.valueOf(i), blockHeight, totalVestedBalance - totalOutlinkStrength - numAccounts + 1));
				} else {
					accounts.add(createAccountWithBalance(String.valueOf(i), blockHeight, 1));
				}
			} else {
				accounts.add(createAccountWithBalance(String.valueOf(i), blockHeight, (totalVestedBalance - totalOutlinkStrength) / numAccounts));
			}
		}

		final SecureRandom sr = new SecureRandom();
		AccountState otherAccount = null;
		for (int i = 0; i < numAccounts; ++i) {
			final AccountState account = accounts.get(i);
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

	private static AccountState createAccountWithBalance(final long numNEM) {
		return createAccountWithBalance(Utils.generateRandomAddress().getEncoded(), 1, numNEM);
	}

	private static AccountState createAccountWithBalance(final String id, final long blockHeight, final long numNEM) {
		final AccountState state = new AccountState(Address.fromEncoded("account" + id));
		state.getWeightedBalances().addFullyVested(new BlockHeight(blockHeight), Amount.fromNem(numNEM));
		return state;
	}

	private static ColumnVector getAccountImportances(
			final BlockHeight blockHeight,
			final Collection<AccountState> accounts) {
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

	private static void outputComparison(
			final String title,
			final double importance1,
			final double importance2) {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final String message = String.format(
				"%s: User1 importance is %s, User2 importance is %s, ratio is %s",
				title,
				format.format(importance1),
				format.format(importance2),
				format.format(importance1 / importance2));
		LOGGER.info(message);
	}
}
