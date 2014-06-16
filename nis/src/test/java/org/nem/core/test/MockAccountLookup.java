package org.nem.core.test;

import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;

import java.util.HashMap;

/**
 * A mock AccountLookup implementation.
 */
public class MockAccountLookup implements AccountLookup {

	private final UnknownAccountBehavior unknownAccountBehavior;
	private int numFindByIdCalls;
	private HashMap<Address, Account> accountMap = new HashMap<>();

	/**
	 * The default behavior of findByAddress if the address is unknown.
	 */
	public enum UnknownAccountBehavior {
		/**
		 * Return a MockAccount (default).
		 */
		MOCK_ACCOUNT,

		/**
		 * Return a real Account.
		 */

		REAL_ACCOUNT,
		/**
		 * Return null.
		 */
		NULL
	}

	/**
	 * Creates a new mock account lookup with the default unknown account behavior.
	 */
	public MockAccountLookup() {
		this(UnknownAccountBehavior.MOCK_ACCOUNT);
	}

	/**
	 * Creates a new mock account lookup that can optionally return null accounts
	 * instead of mock accounts.
	 *
	 * @param unknownAccountBehavior The unknown account behavior.
	 */
	public MockAccountLookup(final UnknownAccountBehavior unknownAccountBehavior) {
		this.unknownAccountBehavior = unknownAccountBehavior;
	}

	@Override
	public Account findByAddress(final Address id) {
		++this.numFindByIdCalls;

		final Account account = this.accountMap.get(id);
		if (null != account)
			return account;

		switch (unknownAccountBehavior) {
			case NULL:
				return null;

			case REAL_ACCOUNT:
			case MOCK_ACCOUNT:
			default:
				return new Account(id);
		}
	}

	@Override
	public boolean isKnownAddress(Address id) {
		return this.accountMap.get(id) != null;
	}

	/**
	 * Returns the number of times findByAddress was called.
	 *
	 * @return The number of times findByAddress was called.
	 */
	public int getNumFindByIdCalls() {
		return this.numFindByIdCalls;
	}

	/**
	 * Sets the account that should be returned by findByAddress.
	 *
	 * @param account The account that should be returned by findByAddress.
	 */
	public void setMockAccount(final Account account) {
		this.accountMap.put(account.getAddress(), account);
	}
}