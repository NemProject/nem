package org.nem.nis;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.Account;
import org.nem.core.model.AccountLink;
import org.nem.core.model.Amount;
import org.nem.core.test.Utils;

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

		// A sends all 400 NEM to B
		a.addOutlink(new AccountLink(100, b));
		
		//, who sends 300 NEM to C, who sends 200 NEM
		// to D, who sends 100 to A.

		// Act: calculate importances

		// Assert:
		// G > E > F >> A > others
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
		POI poi = new POIV1Impl();
		ColumnVector importances = poi.getAccountImportances(accts);

		// Assert:
		Assert.assertTrue(importances.getAt(0) < importances.getAt(2));
		Assert.assertTrue(importances.getAt(0) > importances.getAt(1));
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
}
