package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.websocket.UnconfirmedTransactionListener;

import java.util.Collection;
import java.util.List;

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
	public UnconfirmedTransactionsFilter asFilter() {
		return new SynchronizedUnconfirmedTransactionsFilter(this.unconfirmedTransactions.asFilter(), this.lock);
	}

	private static class SynchronizedUnconfirmedTransactionsFilter implements UnconfirmedTransactionsFilter {
		private final UnconfirmedTransactionsFilter filter;
		private final Object lock;

		public SynchronizedUnconfirmedTransactionsFilter(final UnconfirmedTransactionsFilter filter, final Object lock) {
			this.filter = filter;
			this.lock = lock;
		}

		@Override
		public Collection<Transaction> getAll() {
			synchronized (this.lock) {
				return this.filter.getAll();
			}
		}

		@Override
		public Collection<Transaction> getUnknownTransactions(final Collection<HashShortId> knownHashShortIds) {
			synchronized (this.lock) {
				return this.filter.getUnknownTransactions(knownHashShortIds);
			}
		}

		@Override
		public Collection<Transaction> getMostRecentTransactionsForAccount(final Address address, final int maxTransactions) {
			synchronized (this.lock) {
				return this.filter.getMostRecentTransactionsForAccount(address, maxTransactions);
			}
		}

		@Override
		public Collection<Transaction> getTransactionsBefore(final TimeInstant time) {
			synchronized (this.lock) {
				return this.filter.getTransactionsBefore(time);
			}
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
	public void addListener(final UnconfirmedTransactionListener transactionListener) {
		synchronized (this.lock) {
			this.unconfirmedTransactions.addListener(transactionListener);
		}
	}

	@Override
	public List<UnconfirmedTransactionListener> getListeners() {
		synchronized (this.lock) {
			return this.unconfirmedTransactions.getListeners();
		}
	}

	@Override
	public void removeAll(final Collection<Transaction> transactions) {
		synchronized (this.lock) {
			this.unconfirmedTransactions.removeAll(transactions);
		}
	}

	@Override
	public void dropExpiredTransactions(final TimeInstant time) {
		synchronized (this.lock) {
			this.unconfirmedTransactions.dropExpiredTransactions(time);
		}
	}
}
