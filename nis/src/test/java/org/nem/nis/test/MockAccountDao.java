package org.nem.nis.test;

import org.nem.core.model.*;
import org.nem.core.model.primitive.ReferenceCount;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dbmodel.*;

import java.util.*;

/**
 * A mock AccountDao implementation.
 */
public class MockAccountDao implements AccountDao {
	private int numGetAccountByPrintableAddressCalls;
	private final Map<Address, AccountState> knownAccounts = new HashMap<>();
	private Long id = 1L;

	/**
	 * Gets the number of times getAccountByPrintableAddress was called.
	 *
	 * @return The number of times getAccountByPrintableAddress was called.
	 */
	public int getNumGetAccountByPrintableAddressCalls() {
		return this.numGetAccountByPrintableAddressCalls;
	}

	/**
	 * Adds a mapping between a model address and a db-model account.
	 *
	 * @param address The model address
	 * @param dbAccount The db-model account.
	 */
	public void addMapping(final Address address, final DbAccount dbAccount) {
		if (!this.knownAccounts.containsKey(address)) {
			this.id++;
			this.knownAccounts.put(address, new AccountState(dbAccount));
		}

		this.knownAccounts.get(address).incrementReferenceCount();
	}

	private void decrementReferenceCount(final DbAccount dbAccount) {
		final ReferenceCount referenceCount = this.getAccount(dbAccount).decrementReferenceCount();
		if (ReferenceCount.ZERO.equals(referenceCount)) {
			this.knownAccounts.remove(Address.fromEncoded(dbAccount.getPrintableKey()));
			// this will work because block deletion is always deletion of all blocks after a certain height.
			this.id--;
		}
	}

	/**
	 * Adds a mapping between a model account and a db-model account.
	 *
	 * @param account The model account
	 * @param dbAccount The db-model account.
	 */
	public void addMapping(final org.nem.core.model.Account account, final DbAccount dbAccount) {
		this.addMapping(account.getAddress(), dbAccount);
	}

	/**
	 * Adds all auto mappings in the block.
	 *
	 * @param block The block.
	 */
	public void addMappings(final Block block) {
		this.addMapping(block.getSigner());
		block.getTransactions().stream().flatMap(t -> t.getAccounts().stream()).forEach(this::addMapping);
	}

	/**
	 * Adds an auto mapping for the specified account.
	 *
	 * @param account The account.
	 */
	public void addMapping(final Account account) {
		final DbAccount dbSender = new DbAccount(account.getAddress());
		this.addMapping(account, dbSender);
	}

	/**
	 * Makes a shallow copy of this dao.
	 *
	 * @return A shallow copy.
	 */
	public MockAccountDao shallowCopy() {
		final MockAccountDao copy = new MockAccountDao();
		copy.numGetAccountByPrintableAddressCalls = this.numGetAccountByPrintableAddressCalls;
		copy.id = this.id;
		copy.knownAccounts.putAll(this.knownAccounts);
		return copy;
	}

	// Not exactly what equals should look like but good enough for us.
	public boolean equals(final MockAccountDao rhs) {
		return this.knownAccounts.size() == rhs.knownAccounts.size() && this.id.equals(rhs.id) && this.areKnownAccountsEquivalent(rhs);
	}

	private boolean areKnownAccountsEquivalent(final MockAccountDao rhs) {
		return 0 == this.knownAccounts.values().stream().mapToInt(state -> {
			final DbAccount a1 = state.dbAccount;
			final DbAccount a2 = rhs.getAccount(a1).dbAccount;
			if (!a1.getPrintableKey().equals(a2.getPrintableKey())
					|| (null != a1.getPublicKey() && !a1.getPublicKey().equals(a2.getPublicKey()))) {
				return 1;
			}

			return 0;
		}).sum();
	}

	public void blockAdded(final DbBlock block) {
		this.save(block.getHarvester());
		block.getBlockTransferTransactions().stream().forEach(t -> {
			this.save(t.getRecipient());
			this.save(t.getSender());
		});
	}

	public void blockDeleted(final DbBlock block) {
		this.decrementReferenceCount(block.getHarvester());
		block.getBlockTransferTransactions().stream().forEach(t -> {
			this.decrementReferenceCount(t.getRecipient());
			this.decrementReferenceCount(t.getSender());
		});
	}

	@Override
	public DbAccount getAccountByPrintableAddress(final String printableAddress) {
		++this.numGetAccountByPrintableAddressCalls;
		final AccountState state = this.getAccount(printableAddress);
		return null == state ? null : state.dbAccount;
	}

	private void save(final DbAccount dbAccount) {
		this.addMapping(Address.fromEncoded(dbAccount.getPrintableKey()), dbAccount);
	}

	private AccountState getAccount(final String printableAddress) {
		return this.knownAccounts.get(Address.fromEncoded(printableAddress));
	}

	private AccountState getAccount(final DbAccount dbAccount) {
		return this.getAccount(dbAccount.getPrintableKey());
	}

	private static class AccountState {
		final DbAccount dbAccount;
		ReferenceCount refCount;

		public AccountState(final DbAccount dbAccount) {
			this.dbAccount = dbAccount;
			this.refCount = ReferenceCount.ZERO;
		}

		public void incrementReferenceCount() {
			this.refCount = this.refCount.increment();
		}

		public ReferenceCount decrementReferenceCount() {
			this.refCount = this.refCount.decrement();
			return this.refCount;
		}
	}
}
