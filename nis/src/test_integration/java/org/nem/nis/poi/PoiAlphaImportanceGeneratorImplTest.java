package org.nem.nis.poi;

import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.FormatUtils;
import org.nem.nis.secret.AccountLink;

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
public class PoiAlphaImportanceGeneratorImplTest {
	private static final Logger LOGGER = Logger.getLogger(PoiAlphaImportanceGeneratorImplTest.class.getName());

	private static final int OUTLINK_STRATEGY_NONE = 0;
	private static final int OUTLINK_STRATEGY_RANDOM = 1;
	private static final int OUTLINK_STRATEGY_LOOP = 2;
	private static final int OUTLINK_STRATEGY_LOOP_SELF = 3;
	private static final int OUTLINK_STRATEGY_ALL_TO_ONE = 4;

	private static final double HIGH_TOLERANCE = 0.1;
	private static final double LOW_TOLERANCE = 0.05;

	@Test
	public void threeSimpleAccounts() {
		// Arrange:
		final PoiAccountState a = createAccountWithBalance(100);
		final PoiAccountState b = createAccountWithBalance(100);
		final PoiAccountState c = createAccountWithBalance(100);

		final BlockHeight blockHeight = new BlockHeight(1337);

		// A sends all 100 NEM to B,
		this.addOutlink(a, b, blockHeight, 100);

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
		final PoiAccountState a = createAccountWithBalance(400);
		final PoiAccountState b = createAccountWithBalance(0);
		final PoiAccountState c = createAccountWithBalance(0);
		final PoiAccountState d = createAccountWithBalance(0);

		final PoiAccountState e = createAccountWithBalance(400);
		final PoiAccountState f = createAccountWithBalance(400);
		final PoiAccountState g = createAccountWithBalance(400);

		final BlockHeight blockHeight = new BlockHeight(1337);

		//TODO: we really need the infrastructure for adding coinday-weighted links and updating balances.
		// A sends all 400 NEM to B,
		this.addOutlink(a, b, blockHeight, 400);

		//who sends 300 NEM to C,
		this.addOutlink(b, c, blockHeight, 300);

		//who sends 200 NEM to D,
		this.addOutlink(c, d, blockHeight, 200);

		// who sends 100 to A.
		this.addOutlink(d, a, blockHeight, 100);

		// e sends 100 NEM to g
		this.addOutlink(e, g, blockHeight, 100);

		// g sends 100 NEM to f
		this.addOutlink(g, f, blockHeight, 100);

		final List<PoiAccountState> accts = Arrays.asList(a, b, c, d, e, f, g);

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(blockHeight, accts);
		System.out.println(importances);

		// Assert:
		// G > E > F >> A > others
		Assert.assertTrue(importances.getAt(6) > importances.getAt(4));// g>e
		Assert.assertTrue(importances.getAt(4) > importances.getAt(5));// e>f
		//		Assert.assertTrue(importances.getAt(5) > importances.getAt(0));// f>a // accts with no outlinks have 0 importance as currently designed
		Assert.assertTrue(importances.getAt(0) > importances.getAt(1));// a>b
		Assert.assertTrue(importances.getAt(0) > importances.getAt(2));// a>c
		Assert.assertTrue(importances.getAt(0) > importances.getAt(3));// a>d
	}

	private void addOutlink(final PoiAccountState a, final PoiAccountState b, final BlockHeight blockHeight, final long amount) {
		a.getImportanceInfo().addOutlink(new AccountLink(blockHeight, Amount.fromNem(amount), b.getAddress()));
	}

	@Test
	public void twoAccountsLoopSelfVersusTwoAccountsLoop() {
		LOGGER.info("Self loop vs. normal loop");

		// Arrange:
		// TODO: Loops should be detected.
		final List<PoiAccountState> accounts = new ArrayList<>();
		accounts.addAll(this.createUserAccounts(1, 2, 1000, 1, 500, OUTLINK_STRATEGY_LOOP_SELF));
		accounts.addAll(this.createUserAccounts(1, 2, 1000, 1, 500, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final double ratio = (importances.getAt(0) + importances.getAt(1)) / (importances.getAt(2) + importances.getAt(3));
		System.out.print("Self loop vs. normal loop: User 1 importance is " + format.format(importances.getAt(0) + importances.getAt(1)));
		System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(2) + importances.getAt(3)));
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
		final List<PoiAccountState> accounts = new ArrayList<>();
		for (int i = 2; i < 10; i++) {
			accounts.clear();
			accounts.addAll(this.createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(this.createUserAccounts(1, i, 800, 1, 400, OUTLINK_STRATEGY_LOOP));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			final double ratio = importances.getAt(0) / (importances.sum() - importances.getAt(0));
			System.out.print("1 vs. " + i + ": User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(importances.sum() - importances.getAt(0)));
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
		for (int i = 0; i < 400; i = i + 40) {
			accounts.clear();
			accounts.addAll(this.createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(this.createUserAccounts(1, 8, 800, 1, 400, OUTLINK_STRATEGY_LOOP));
			accounts.addAll(this.createUserAccounts(1, i, i * 50, 0, 0, OUTLINK_STRATEGY_NONE));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			double user2Importance = 0;
			for (int j = 1; j < 9; j++) {
				user2Importance += importances.getAt(j);
			}
			final double ratio = importances.getAt(0) / user2Importance;
			System.out.print("1 vs. 8 with " + i + " small lazy accounts: User 1 importance is " + format.format(importances.getAt(0)));
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
		final List<PoiAccountState> accounts = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			accounts.clear();
			accounts.addAll(this.createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(this.createUserAccounts(1, 8, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(this.createUserAccounts(1, i, i * 8000, 0, 0, OUTLINK_STRATEGY_NONE));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			double user2Importance = 0;
			for (int j = 1; j < 9; j++) {
				user2Importance += importances.getAt(j);
			}
			final double ratio = importances.getAt(0) / user2Importance;
			System.out.print("1 vs. 8 with " + i + " big lazy account: User 1 importance is " + format.format(importances.getAt(0)));
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
			accounts.addAll(this.createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(this.createUserAccounts(1, 1, 800, i, 400, OUTLINK_STRATEGY_LOOP_SELF));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			final double ratio = importances.getAt(0) / importances.getAt(1);
			System.out.print("1 outlink vs. " + i + " outlinks: User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(1)));
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
		accounts.addAll(this.createUserAccounts(1, 2, 1000, 1, 500, OUTLINK_STRATEGY_LOOP));
		accounts.addAll(this.createUserAccounts(1, 2, 1000, 1, 50, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final double ratio = (importances.getAt(0) + importances.getAt(1)) / (importances.getAt(2) + importances.getAt(3));
		System.out.print("High outlink strength vs. low outlink strength: User 1 importance is " + format.format(importances.getAt(0) + importances.getAt(1)));
		System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(2) + importances.getAt(3)));
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
		accounts.addAll(this.createUserAccounts(1, 2, 1000000, 1, 500, OUTLINK_STRATEGY_LOOP));
		accounts.addAll(this.createUserAccounts(1, 2, 1000, 1, 500, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final double ratio = (importances.getAt(0) + importances.getAt(1)) / (importances.getAt(2) + importances.getAt(3));
		System.out.print("High balance vs. low balance: User 1 importance is " + format.format(importances.getAt(0) + importances.getAt(1)));
		System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(2) + importances.getAt(3)));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("");

		// Assert
		Assert.assertTrue(ratio > 10.0);
	}

	@Test
	public void poiIsFairerThanPOS() {
		LOGGER.info("Check that POI distributes importance differently than POS");
		//TODO: I don't know why the outlinks vectors are the same for the two groups of accounts below.
		// Arrange:
		// Accounts with smaller vested balance should be able to have more importance than accounts with high balance and low activity
		final int numAccounts = 10;

		final List<PoiAccountState> accounts = new ArrayList<>();
		accounts.addAll(this.createUserAccounts(1, numAccounts, 10000, 1, 500, OUTLINK_STRATEGY_RANDOM));
		accounts.addAll(this.createUserAccounts(1, numAccounts, 1000, 10, 500, OUTLINK_STRATEGY_RANDOM));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();

		double highBalanceSum = 0;
		double lowBalanceSum = 0;

		for (int ndx = 0; ndx < numAccounts; ndx++) {
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
		Assert.assertTrue(ratio > 1.0);
	}

	@Test
	public void accountCannotBoostPOIWithVeryLowBalance() {
		LOGGER.info("Check that an account can't just send most of their balance to another account to boost their score");
		// Arrange:
		// Accounts should not just be able to transfer all their balance to another account to boost their score
		final List<PoiAccountState> accounts = new ArrayList<>();
		accounts.addAll(this.createUserAccounts(1, 2, 10000, 2, 100, OUTLINK_STRATEGY_LOOP));
		accounts.addAll(this.createUserAccounts(1, 2, 10000, 2, 9900, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final double ratio = (importances.getAt(0) + importances.getAt(1)) / (importances.getAt(2) + importances.getAt(3));
		System.out.print("High balance vs. low balance: User 1 importance is " + format.format(importances.getAt(0) + importances.getAt(1)));
		System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(2) + importances.getAt(3)));
		System.out.println(", ratio is " + format.format(ratio));
		System.out.println("Importances: " + importances);
		System.out.println("");

		// Assert
		Assert.assertTrue(ratio > .95);
	}

	@Test
	public void pushOneAccountDoesNotInfluenceImportanceDistribution() {
		LOGGER.info("1 account with 1 outlink vs. many accounts with outlinks to one account (same cumulative strength)");

		// Arrange 1 vs many, the latter concentrate the strength to one account:
		// Colluding accounts that try to push one account with many links should have no influence on the importance distribution.
		final List<PoiAccountState> accounts = new ArrayList<>();
		for (int i = 4; i < 40; i++) {
			accounts.clear();
			accounts.addAll(this.createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(this.createUserAccounts(1, i, 800, 1, 400, OUTLINK_STRATEGY_ALL_TO_ONE));

			// Act: calculate importances
			final ColumnVector importances = getAccountImportances(new BlockHeight(1), accounts);
			//			System.out.println("importances: " + importances);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			final double ratio = importances.getAt(0) / (importances.sum() - importances.getAt(0));
			System.out.print("1 vs. " + i + ", outlink directed to one account: User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(importances.sum() - importances.getAt(0)));
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
		final int numAccounts = 5000;
		final List<PoiAccountState> accounts = new ArrayList<>();
		accounts.addAll(this.createUserAccounts(1, numAccounts, 1000, 1, 500, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		System.out.println("Starting poi calculation.");
		final long start = System.currentTimeMillis();
		getAccountImportances(new BlockHeight(1), accounts);
		final long stop = System.currentTimeMillis();
		System.out.println("Finished poi calculation.");

		System.out.println("For " + numAccounts + " accounts the poi calculation needed " + (stop - start) + "ms.");

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

		final BlockHeight height = new BlockHeight(1);
		long prevTimeDiff = -1;
		for (int numAccounts = 5; numAccounts < 10000; numAccounts *= 10) {
			// Arrange:
			final List<PoiAccountState> accounts = new ArrayList<>();
			accounts.addAll(this.createUserAccounts(1, numAccounts, 1000, 1, 500, OUTLINK_STRATEGY_LOOP));

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

	private List<PoiAccountState> createUserAccounts(final long blockHeight, final int numAccounts, final long totalVestedBalance, final int numOutlinksPerAccount, final long totalOutlinkStrength, final int outlinkStrategy) {
		final List<PoiAccountState> accounts = new ArrayList<>();

		for (int i = 0; i < numAccounts; i++) {
			if (outlinkStrategy == OUTLINK_STRATEGY_ALL_TO_ONE) {
				if (i == 0) {
					accounts.add(createAccountWithBalance(blockHeight, totalVestedBalance - totalOutlinkStrength - numAccounts + 1));
				} else {
					accounts.add(createAccountWithBalance(blockHeight, 1));
				}
			} else {
				accounts.add(createAccountWithBalance(blockHeight, (totalVestedBalance - totalOutlinkStrength) / numAccounts));
			}
		}

		final SecureRandom sr = new SecureRandom();
		PoiAccountState otherAccount = null;
		for (int i = 0; i < numAccounts; i++) {
			final PoiAccountState account = accounts.get(i);
			for (int j = 0; j < numOutlinksPerAccount; j++) {
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
		return createAccountWithBalance(1, numNEM);
	}

	private static PoiAccountState createAccountWithBalance(final long blockHeight, final long numNEM) {
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
		state.getWeightedBalances().addFullyVested(new BlockHeight(blockHeight), Amount.fromNem(numNEM));
		return state;
	}

	private static ColumnVector getAccountImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accounts) {
		final PoiImportanceGenerator poi = new PoiAlphaImportanceGeneratorImpl();
		poi.updateAccountImportances(blockHeight, accounts);
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
