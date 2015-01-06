package org.nem.nis.test;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.ReferenceCount;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dbmodel.*;

import java.util.*;

/**
 * A mock AccountDao implementation.
 */
public class MockAccountDao implements AccountDao {

	private int numGetAccountByPrintableAddressCalls;
	private final Map<String, DbAccount> knownAccounts = new HashMap<>();
	private final Map<DbAccount, ReferenceCount> refCounts = new HashMap<>();
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
		if (null == this.knownAccounts.putIfAbsent(address.getEncoded(), dbAccount)) {
			this.id++;
			this.refCounts.put(dbAccount, ReferenceCount.ZERO);
		}

		this.incrementReferenceCount(this.knownAccounts.get(address.getEncoded()));
	}

	private ReferenceCount incrementReferenceCount(final DbAccount dbAccount) {
		final ReferenceCount referenceCount = this.refCounts.get(dbAccount).increment();
		this.refCounts.put(dbAccount, referenceCount);
		return referenceCount;
	}

	private ReferenceCount decrementReferenceCount(final DbAccount dbAccount) {
		final ReferenceCount referenceCount = this.refCounts.get(dbAccount).decrement();
		if (ReferenceCount.ZERO.equals(referenceCount)) {
			this.refCounts.remove(dbAccount);
			this.knownAccounts.remove(dbAccount.getPrintableKey());
			// this will work because block deletion is always deletion of all blocks after a certain height.
			this.id--;
		} else {
			this.refCounts.put(dbAccount, referenceCount);
		}

		return referenceCount;
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

	public MockAccountDao shallowCopy() {
		final MockAccountDao copy = new MockAccountDao();
		copy.numGetAccountByPrintableAddressCalls = this.numGetAccountByPrintableAddressCalls;
		copy.id = this.id;
		copy.knownAccounts.putAll(this.knownAccounts);
		copy.refCounts.putAll(this.refCounts);
		return copy;
	}

	// Not exactly what equals should look like but good enough for us.
	public boolean equals(final MockAccountDao rhs) {
		if (this.knownAccounts.size() != rhs.knownAccounts.size() || !this.id.equals(rhs.id)) {
			return false;
		}

		return 0 == this.knownAccounts.values().stream()
				.mapToInt(a1 -> {
					final DbAccount a2 = rhs.knownAccounts.get(a1.getPrintableKey());
					if (!a1.getPrintableKey().equals(a2.getPrintableKey()) ||
							(null != a1.getPublicKey() && !a1.getPublicKey().equals(a2.getPublicKey()))) {
						return 1;
					}

					return 0;
				})
				.sum();
	}

	public void blockAdded(final DbBlock block) {
		this.save(block.getForger());
		block.getBlockTransferTransactions().stream()
				.forEach(t -> {
					this.save(t.getRecipient());
					this.save(t.getSender());
				});
	}

	public void blockDeleted(final DbBlock block) {
		this.decrementReferenceCount(block.getForger());
		block.getBlockTransferTransactions().stream()
				.forEach(t -> {
					this.decrementReferenceCount(t.getRecipient());
					this.decrementReferenceCount(t.getSender());
				});
	}

	public Iterator<DbAccount> iterator() {
		return this.knownAccounts.values().iterator();
	}

	@Override
	public DbAccount getAccount(final Long id) {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public DbAccount getAccountByPrintableAddress(final String printableAddress) {
		++this.numGetAccountByPrintableAddressCalls;
		return this.knownAccounts.get(printableAddress);
	}

	@Override
	public void save(final DbAccount dbAccount) {
		this.addMapping(Address.fromEncoded(dbAccount.getPrintableKey()), dbAccount);
	}
}