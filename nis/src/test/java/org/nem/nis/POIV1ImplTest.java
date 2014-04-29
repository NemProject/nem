package org.nem.nis;

import java.util.ArrayList;
import java.util.List;

import org.nem.core.model.Account;
import org.nem.core.model.Amount;
import org.nem.core.test.Utils;

/**
 * If someone can manipulate their importance so that they can often or at-will
 * be chosen to forage, then things like double-spend attacks become possible.
 * Thus the tests considered here focus on verifying that a user cannot
 * arbitrarily manipulate their importance to cause them to be chosen to forage.
 * 
 * some tests we should consider: 
 * - Sybil attack (master node creates a ton of
 * other nodes and transacts with them (and maybe some other nodes) to try to
 * boost score)</br>
 * - infinite loop attack<br/>
 * - closed loop attack<br/>
 * - small transaction spam attack<br/> 
 * -
 */
public class POIV1ImplTest {

	private List<Account> getAccountsWithSameBalance(int numAccounts,
			long balance) {
		List<Account> accounts = new ArrayList<Account>();

		for (int ndx = 0; ndx < numAccounts; ndx++) {
			Account acct = createAccountWithBalance(balance);
			accounts.add(acct);
		}

		return accounts;
	}

	private static Account createAccountWithBalance(long balance) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(balance));
		return account;
	}
}
