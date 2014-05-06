package org.nem.nis;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.Account;
import org.nem.core.model.AccountLink;
import org.nem.core.model.Amount;
import org.nem.core.model.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.utils.FormatUtils;
import org.nem.nis.test.MockAccount;

/**
 * If someone can manipulate their importance so that they can often or at-will
 * be chosen to forage, then things like double-spend attacks become possible.
 * Thus the tests considered here focus on verifying that a user cannot
 * arbitrarily manipulate their importance to cause them to be chosen to forage.
 * 
 * some tests we should consider: - Sybil attack (master node creates a ton of
 * other nodes and transacts with them (and maybe some other nodes) to try to
 * boost score)</br> - infinite loop attack<br/>
 * - closed loop attack<br/>
 * - small transaction spam attack<br/>
 * -
 */
public class POIV1ImplTest {
	private static final Logger LOGGER = Logger.getLogger(POIV1ImplTest.class.getName());

	static private final int OUTLINK_STRATEGY_NONE = 0;
	static private final int OUTLINK_STRATEGY_RANDOM = 1;
	static private final int OUTLINK_STRATEGY_LOOP = 2;
	static private final int OUTLINK_STRATEGY_LOOP_SELF = 3;
	
	/**
	 * Four nodes (A, B, C, D) are owned by one person with 400 NEM who distributed the NEM 
	between the nodes and cycled the NEM around. The other three nodes are independent and have 400 NEM each.
	
	The following transactions occur (transaction fees are assumed to be 0):
	A, E, F, G all start with 400 NEM; ABCD are all controlled by actor A.
	A sends all 400 NEM to B, who sends 300 NEM to C, who sends 200 NEM to D, who sends 100 to A.
	
	E starts with 400 NEM and sends 100 to G.
	G starts with 400 NEM, gets 100 from E, and sends 100 to F.
	 */
	@Test
	public void fourNodeSimpleLoopAttack() {

		// Arrange:
		Account a = createAccountWithBalance(400);
		Account b = createAccountWithBalance(0);
		Account c = createAccountWithBalance(0);
		Account d = createAccountWithBalance(0);

		Account e = createAccountWithBalance(400);
		Account f = createAccountWithBalance(400);
		Account g = createAccountWithBalance(400);
		
		final BlockHeight blockHeight = new BlockHeight(1337);

		//TODO: we really need the infrastructure for adding coinday-weighted links and updating balances.
		// A sends all 400 NEM to B,
		a.addOutlink(new AccountLink(400, b));
		
		//who sends 300 NEM to C,
		b.addOutlink(new AccountLink(300, c));
		
		//who sends 200 NEM to D,
		c.addOutlink(new AccountLink(200, d));
				
		// who sends 100 to A.
		d.addOutlink(new AccountLink(100, a));
		
		List<Account> accts = Arrays.asList(a, b, c, d, e, f, g);

		// Act: calculate importances
		POI poi = new POIV1Impl();
		ColumnVector importances = poi.getAccountImportances(blockHeight, accts);

		// Assert:
		// G > E > F >> A > others
		Assert.assertTrue(importances.getAt(6) > importances.getAt(4));// g>e
		Assert.assertTrue(importances.getAt(4) > importances.getAt(5));// e>f
		Assert.assertTrue(importances.getAt(5) > importances.getAt(0));// f>a
		Assert.assertTrue(importances.getAt(0) > importances.getAt(1));// a>b
		Assert.assertTrue(importances.getAt(0) > importances.getAt(2));// a>c
		Assert.assertTrue(importances.getAt(0) > importances.getAt(3));// a>d
	}

	/**
	 * Super quick
	 */
	@Test
	public void superQuickHowToRunPOIHack() {

		// Arrange:
		List<Account> accts = getAccountsWithSameBalance(100, 1337);

		// acct 0 sends 100 NEM to acct 2

		// TODO: This is the type of thing we need to do to hook transactions up
		// into
		// POI. The "strengths" need to be coinday-weighted, meaning that if you
		// send 100 NEM to someone, you better have at least 100 coindays in
		// your account. If not, then some fraction is sent. After sending NEM
		// to someone, your own coindays need to be recalculated.
		accts.get(0).addOutlink(new AccountLink(100, accts.get(2)));

		// Act:
		// A sends all 400 NEM to B, who sends 300 NEM to C, who sends 200 NEM
		// to D, who sends 100 to A.
		
		final BlockHeight blockHeight = new BlockHeight(1337);
		POI poi = new POIV1Impl();
		ColumnVector importances = poi.getAccountImportances(blockHeight, accts);

		// Assert:
		Assert.assertTrue(importances.getAt(0) < importances.getAt(2));
		Assert.assertTrue(importances.getAt(0) > importances.getAt(1));
	}

	@Test
	public void twoAccountsLoopSelfVersusTwoAccountsLoop() {
		LOGGER.info("Self loop vs. normal loop");
		
		// Arrange:
		// Sending nem to your own account should not be considered as real transaction,
		// i.e. the account should not collect coin days.
		List<Account> accounts = new ArrayList<Account>();
		accounts.addAll(createUserAccounts(1, 2, 1000, 1, 500, OUTLINK_STRATEGY_LOOP_SELF));
		accounts.addAll(createUserAccounts(1, 2, 1000, 1, 500, OUTLINK_STRATEGY_LOOP));

		// Act: calculate importances
		POI poi = new POIV1Impl();
		ColumnVector importances = poi.getAccountImportances(new BlockHeight(1), accounts);

		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		System.out.print("Self loop vs. normal loop: User 1 importance is " + format.format(importances.getAt(0) + importances.getAt(1)));
		System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(2) + importances.getAt(3)));
		System.out.println(", ratio is " + format.format((importances.getAt(0) + importances.getAt(1))/(importances.getAt(2) + importances.getAt(3))));
		System.out.println("");
	}
	
	@Test
	public void oneAccountLoopSelfVersusManyAccountsLoop() {
		LOGGER.info("1 account vs. many accounts");

		// Arrange 1 vs many:
		// Many accounts have a high importance after the power iteration due to the large inverse teleportation coefficient
		// but get crushed in the final calculation.
		List<Account> accounts = new ArrayList<Account>();
		for (int i=2; i<10; i++) {
			accounts.clear();
			accounts.addAll(createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(createUserAccounts(1, i, 800, 1, 400, OUTLINK_STRATEGY_LOOP));
	
			// Act: calculate importances
			POI poi = new POIV1Impl();
			ColumnVector importances = poi.getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			System.out.print("1 vs. " + i + ": User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(importances.sum() - importances.getAt(0)));
			System.out.println(", ratio is " + format.format(importances.getAt(0)/(importances.sum() - importances.getAt(0))));
		}
		System.out.println("");
	}
	
	@Test
	public void manySmallLazyAcountsInfluenceImportanceDistribution() {
		LOGGER.info("1 account vs. 8 accounts with many lazy accounts");

		// Arrange 1 vs 8, with lazy accounts:
		// The presence of many small lazy accounts has some influence on the importance distribution
		// but less than I expected.
		List<Account> accounts = new ArrayList<Account>();
		for (int i=0; i<400; i=i+40) {
			accounts.clear();
			accounts.addAll(createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(createUserAccounts(1, 8, 800, 1, 400, OUTLINK_STRATEGY_LOOP));
			accounts.addAll(createUserAccounts(1, i, i*20, 0, 0, OUTLINK_STRATEGY_NONE));
	
			// Act: calculate importances
			POI poi = new POIV1Impl();
			ColumnVector importances = poi.getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			double user2Importance = 0;
			for (int j=1; j<9; j++) {
				user2Importance += importances.getAt(j);
			}
			System.out.print("1 vs. 8 with " + i + " small lazy accounts: User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(user2Importance));
			System.out.println(", ratio is " + format.format(importances.getAt(0)/user2Importance));
		}
		System.out.println("");
	}
	
	@Test
	public void oneBigLazyAcountInfluencesImportanceDistribution() {
		LOGGER.info("1 account vs. 8 accounts with 0 or 1 big lazy account");

		// Arrange 1 vs 8, with 0 or 1 lazy account:
		// The presence of a big lazy account has huge influence on the importance distribution,
		// Account 1's inverse teleportation vector gets boosted.
		List<Account> accounts = new ArrayList<Account>();
		for (int i=0; i<2; i++) {
			accounts.clear();
			accounts.addAll(createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(createUserAccounts(1, 8, 800, 1, 400, OUTLINK_STRATEGY_LOOP));
			accounts.addAll(createUserAccounts(1, i, i*800, 0, 0, OUTLINK_STRATEGY_NONE));
	
			// Act: calculate importances
			POI poi = new POIV1Impl();
			ColumnVector importances = poi.getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			double user2Importance = 0;
			for (int j=1; j<9; j++) {
				user2Importance += importances.getAt(j);
			}
			System.out.print("1 vs. 8 with " + i + " big lazy account: User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(user2Importance));
			System.out.println(", ratio is " + format.format(importances.getAt(0)/user2Importance));
		}
		System.out.println("");
	}
	
	@Test
	public void numberOfOutlinksInfluencesImportanceDistribution() {
		LOGGER.info("1 account with 1 outlink vs. 1 account many outlinks (same cumulative strength)");

		// Arrange 1 vs 1, the latter distributes the strength to many outlinks:
		// One big transaction (1 outlink) is much better than many small transactions (many outlinks),
		// this is due to using the median in the outlink score calculation.
		List<Account> accounts = new ArrayList<Account>();
		for (int i=1; i<10; i++) {
			accounts.clear();
			accounts.addAll(createUserAccounts(1, 1, 800, 1, 400, OUTLINK_STRATEGY_LOOP_SELF));
			accounts.addAll(createUserAccounts(1, 1, 800, i, 400, OUTLINK_STRATEGY_LOOP_SELF));
	
			// Act: calculate importances
			POI poi = new POIV1Impl();
			ColumnVector importances = poi.getAccountImportances(new BlockHeight(1), accounts);
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			System.out.print("1 outlink vs. " + i + " outlinks: User 1 importance is " + format.format(importances.getAt(0)));
			System.out.print(", User 2 cumulative importance is " + format.format(importances.getAt(1)));
			System.out.println(", ratio is " + format.format(importances.getAt(0)/importances.getAt(1)));
		}
		System.out.println("");
	}
	
	private List<MockAccount> createUserAccounts(long blockHeight, int numAccounts, long totalBalance, int numOutLinksPerAccount, long totalOutLinkStrength, int outLinkStrategy) {
		List<MockAccount> accounts = new ArrayList<MockAccount>();
		
		for (int i=0; i<numAccounts; i++) {
			accounts.add(createMockAccountWithBalance((totalBalance - totalOutLinkStrength)/numAccounts));
		}
		
		SecureRandom sr = new SecureRandom();
		MockAccount otherAccount = null;
		for (int i=0; i<numAccounts; i++) {
			MockAccount account = accounts.get(i);
			account.setCoinDaysAt(account.getBalance(), new BlockHeight(blockHeight));
			for (int j=0; j< numOutLinksPerAccount; j++) {
				switch (outLinkStrategy) {
				case OUTLINK_STRATEGY_RANDOM:
					otherAccount = accounts.get(sr.nextInt(numAccounts));
					break;
				case OUTLINK_STRATEGY_LOOP:
					otherAccount = accounts.get((i+1) % numAccounts);
					break;
				case OUTLINK_STRATEGY_LOOP_SELF:
					otherAccount = account;
					break;
				}
				account.addOutlink(new AccountLink(Amount.fromNem(totalOutLinkStrength/(numAccounts*numOutLinksPerAccount)).getNumMicroNem(), otherAccount));
			}
		}
		
		return accounts;
	}
	
	private List<Account> getAccountsWithSameBalance(int numAccounts, long numNEM) {
		List<Account> accounts = new ArrayList<Account>();

		for (int ndx = 0; ndx < numAccounts; ndx++) {
			Account acct = createAccountWithBalance(numNEM);
			accounts.add(acct);
		}

		return accounts;
	}

	private static Account createAccountWithBalance(long numNEM) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(numNEM));
		return account;
	}

	private static MockAccount createMockAccountWithBalance(long numNEM) {
		final MockAccount account = new MockAccount();
		account.incrementBalance(Amount.fromNem(numNEM));
		return account;
	}
}
