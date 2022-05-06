package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.websocket.UnconfirmedTransactionListener;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A collection of unconfirmed transactions.
 */
public class DefaultUnconfirmedTransactions implements UnconfirmedTransactions {
	private static final Logger LOGGER = Logger.getLogger(DefaultUnconfirmedTransactions.class.getName());

	private final UnconfirmedStateFactory unconfirmedStateFactory;
	private final ReadOnlyNisCache nisCache;
	private final UnconfirmedTransactionsCache transactions;
	private final UnconfirmedTransactionsFilter transactionsFilter;
	private UnconfirmedState state;

	/**
	 * Creates a new unconfirmed transactions collection.
	 *
	 * @param unconfirmedStateFactory The unconfirmed state factory to use.
	 * @param nisCache The NIS cache to use.
	 */
	public DefaultUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory, final ReadOnlyNisCache nisCache) {
		this.unconfirmedStateFactory = unconfirmedStateFactory;
		this.nisCache = nisCache;

		final MultisigSignatureMatchPredicate matchPredicate = new MultisigSignatureMatchPredicate(nisCache.getAccountStateCache());
		this.transactions = new UnconfirmedTransactionsCache(matchPredicate::isMatch);
		this.transactionsFilter = new DefaultUnconfirmedTransactionsFilter(this.transactions,
				new ImpactfulTransactionPredicate(nisCache.getAccountStateCache()));

		this.resetState();
	}

	@Override
	public int size() {
		return this.transactions.size();
	}

	@Override
	public Amount getUnconfirmedBalance(final Account account) {
		return this.state.getUnconfirmedBalance(account);
	}

	@Override
	public Quantity getUnconfirmedMosaicBalance(final Account account, final MosaicId mosaicId) {
		return this.state.getUnconfirmedMosaicBalance(account, mosaicId);
	}

	@Override
	public ValidationResult addNewBatch(final Collection<Transaction> transactions) {
		return this.state.addNewBatch(transactions);
	}

	@Override
	public ValidationResult addNew(final Transaction transaction) {
		return this.state.addNew(transaction);
	}

	@Override
	public ValidationResult addExisting(final Transaction transaction) {
		return this.state.addExisting(transaction);
	}

	/**
	 * Adds a transaction listener to unconfirmed transactions state. Listener will be informed about incoming unconfirmed transactions that
	 * passed the validation.
	 *
	 * @param transactionListener The unconfirmed transaction listener.
	 */
	@Override
	public void addListener(final UnconfirmedTransactionListener transactionListener) {
		this.state.addListener(transactionListener);
	}

	@Override
	public List<UnconfirmedTransactionListener> getListeners() {
		return this.state.getListeners();
	}

	@Override
	public void removeAll(final Collection<Transaction> transactions) {
		// (1) remove all matching transactions from the cache
		transactions.forEach(this.transactions::remove);

		// (2) copy the remaining transactions
		final Collection<Transaction> notRemovedTransactions = this.asFilter().getAll();

		// (3) reset the state
		this.resetState(notRemovedTransactions);
	}

	@Override
	public void dropExpiredTransactions(final TimeInstant time) {
		final List<Transaction> notExpiredTransactions = this.transactions.stream().filter(tx -> !this.isExpired(tx, time))
				.collect(Collectors.toList());
		if (notExpiredTransactions.size() == this.transactions.size()) {
			return;
		}

		LOGGER.info("expired unconfirmed transaction in cache detected, rebuilding cache");
		this.resetState(notExpiredTransactions);
	}

	private boolean isExpired(final Transaction transaction, final TimeInstant time) {
		return TransactionExtensions.streamDefault(transaction).anyMatch(t -> t.getDeadline().compareTo(time) < 0);
	}

	@Override
	public UnconfirmedTransactionsFilter asFilter() {
		return this.transactionsFilter;
	}

	private void resetState() {
		this.resetState(Collections.emptyList());
	}

	private void resetState(final Collection<Transaction> transactions) {
		// reset the state
		this.transactions.clear();
		final List<UnconfirmedTransactionListener> listeners = this.state != null ? this.state.getListeners() : null;
		this.state = this.unconfirmedStateFactory.create(this.nisCache.copy(), this.transactions);

		// replay all transactions
		transactions.forEach(this.state::addExisting);

		if (listeners != null) {
			listeners.forEach(this.state::addListener);
		}
	}
}
