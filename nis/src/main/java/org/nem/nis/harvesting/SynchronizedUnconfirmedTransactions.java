package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A synchronized UnconfirmedTransactions implementation.
 */
public class SynchronizedUnconfirmedTransactions implements UnconfirmedTransactions {
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param unconfirmedTransactions The unconfirmed transactions.
	 */
	public SynchronizedUnconfirmedTransactions(final UnconfirmedTransactions unconfirmedTransactions) {
		this.unconfirmedTransactions = unconfirmedTransactions;
	}

	@Override
	public List<Transaction> getAll() {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.getAll();
		}
	}

	@Override
	public List<Transaction> getUnknownTransactions(final Collection<HashShortId> knownHashShortIds) {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.getUnknownTransactions(knownHashShortIds);
		}
	}

	@Override
	public List<Transaction> getMostRecentTransactionsForAccount(final Address address, final int maxTransactions) {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.getMostRecentTransactionsForAccount(address, maxTransactions);
		}
	}

	@Override
	public List<Transaction> getTransactionsBefore(final TimeInstant time) {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.getTransactionsBefore(time);
		}
	}

	@Override
	public int size() {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.size();
		}
	}

	@Override
	public Amount getUnconfirmedBalance(final Account account) {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.getUnconfirmedBalance(account);
		}
	}

	@Override
	public Quantity getUnconfirmedMosaicBalance(final Account account, final MosaicId mosaicId) {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.getUnconfirmedMosaicBalance(account, mosaicId);
		}
	}

	@Override
	public ValidationResult addNewBatch(final Collection<Transaction> transactions) {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.addNewBatch(transactions);
		}
	}

	@Override
	public ValidationResult addNew(final Transaction transaction) {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.addNew(transaction);
		}
	}

	@Override
	public ValidationResult addExisting(final Transaction transaction) {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.addExisting(transaction);
		}
	}

	@Override
	public boolean remove(final Transaction transaction) {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.remove(transaction);
		}
	}

	@Override
	public void removeAll(final Block block) {
		synchronized (this.lock) {
			this.unconfirmedTransactions.removeAll(block);
		}
	}

	@Override
	public void dropExpiredTransactions(final TimeInstant time) {
		synchronized (this.lock) {
			this.unconfirmedTransactions.dropExpiredTransactions(time);
		}
	}
}