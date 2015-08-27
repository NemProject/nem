package org.nem.nis.harvesting;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.*;
import org.nem.core.time.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.secret.*;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A collection of unconfirmed transactions.
 */
public class DefaultUnconfirmedTransactions implements UnconfirmedTransactions {
	private static final Logger LOGGER = Logger.getLogger(DefaultUnconfirmedTransactions.class.getName());

	private final UnconfirmedTransactionsCache transactions;
	private final UnconfirmedTransactionsFilter transactionsFilter;
	private final UnconfirmedBalancesObserver unconfirmedBalances;
	private final UnconfirmedMosaicBalancesObserver unconfirmedMosaicBalances;
	private final TransactionObserver transferObserver;
	private final DefaultUnconfirmedState state;

	/**
	 * Creates a new unconfirmed transactions collection.
	 *
	 * @param validatorFactory The transaction validator factory to use.
	 * @param nisCache The NIS cache to use.
	 * @param timeProvider The time provider.
	 * @param blockHeightSupplier Supplier that provides the current block height.
	 */
	public DefaultUnconfirmedTransactions(
			final TransactionValidatorFactory validatorFactory,
			final ReadOnlyNisCache nisCache,
			final TimeProvider timeProvider,
			final Supplier<BlockHeight> blockHeightSupplier) {
		this.unconfirmedBalances = new UnconfirmedBalancesObserver(nisCache.getAccountStateCache());
		this.unconfirmedMosaicBalances = new UnconfirmedMosaicBalancesObserver(nisCache.getNamespaceCache());
		this.transferObserver = this.createObserver();

		final MultisigSignatureMatchPredicate matchPredicate = new MultisigSignatureMatchPredicate(nisCache.getAccountStateCache());
		this.transactions = new UnconfirmedTransactionsCache(matchPredicate::isMatch);
		this.transactionsFilter = new DefaultUnconfirmedTransactionsFilter(
				this.transactions,
				new ImpactfulTransactionPredicate(nisCache.getAccountStateCache()));

		final UnconfirmedStateFactory factory = new UnconfirmedStateFactory(
				validatorFactory,
				cache -> new TransactionObserverToBlockTransferObserverAdapter(this.transferObserver),
				timeProvider,
				blockHeightSupplier);
		this.state = factory.create(nisCache.copy(), this.transactions);
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

	private TransactionObserver createObserver() {
		final AggregateTransactionObserverBuilder builder = new AggregateTransactionObserverBuilder();
		builder.add(this.unconfirmedBalances);
		builder.add(this.unconfirmedMosaicBalances);
		return builder.build();
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

	@Override
	public boolean remove(final Transaction transaction) {
		final boolean isRemoved = this.transactions.remove(transaction);
		if (isRemoved) {
			transaction.undo(this.transferObserver);
		}

		return isRemoved;
	}

	@Override
	public void removeAll(final Collection<Transaction> transactions) {
		// This is ugly but we cannot let an exception bubble up because NIS would need a restart.
		// The exception means we have to rebuild the cache because it is corrupt.
		// This can happen during synchronization. Consider the following scenario:
		// The complete blockchain has height y and our blockchain has height x < y.
		// At height y account A has 100 NEM confirmed balance and at height x it has 0 NEM confirmed balance.
		// Another account B has enough NEM for transactions at all considered heights.
		// Our node receives an unconfirmed tx T1: B -> A 10 NEM (+1 NEM fee) which is old and was included in the block at height x + 1.
		// Our node does not know about block x + 1 yet so it accepts T1 as unconfirmed transaction.
		// Our node pulls block x + 1 via synchronizeNode(). After validation, execution and committing the NisCache
		// the unconfirmed balance of account A that our node sees is
		// unconfirmed = 10 NEM (confirmed) + 10 NEM (still credited amount) - 0 NEM (debited amount) = 20 NEM.
		// At this point (i.e. before removeAll() is called for block x + 1) our node receives another unconfirmed transaction
		// T2: A -> B 15 NEM (+1 NEM fee). The transaction is valid since our node sees 20 NEM as unconfirmed balance for account A and thus is accepted.
		// Our node now sees 4 NEM as unconfirmed balance for account A
		// Now removeAll() is called for block x + 1 and this triggers T1.undo() which results in an illegal argument exception (4 - 10 < 0).
		// The scenario shows that exceptions can occur in a natural way and we therefore ought to catch them here.
		try {
			// undo in reverse order!
			for (final Transaction transaction : getReverseTransactions(transactions)) {
				this.remove(transaction);
			}
		} catch (final NegativeBalanceException e) {
			LOGGER.severe("illegal argument exception during removal of unconfirmed transactions, rebuilding cache");
			this.rebuildCache(this.asFilter().getAll());
			return;
		}

		// Consider the following scenario:
		// Account A has 10 NEM and sends tx 1 (A -> B 8 NEM) to node 1 and tx2 (A -> B 9 NEM) to node 2.
		// UnconfirmedBalancesObserver of node 1 sees for A: balance 10, credited 0, debited 8 -> unconfirmed balance is 2
		// Now node 2 harvests a block and includes tx2. node 1 receives + executes the block with tx2.
		// UnconfirmedBalancesObserver of node 1 sees for A: balance 1, credited 0, debited 8 -> unconfirmed balance is -7
		// Next call to UnconfirmedBalancesObserver.get(A) results in an exception.
		// This means a new block can ruin the unconfirmed balance. We have to check if all balances are still valid.
		if (!this.areUnconfirmedBalancesValid()) {
			LOGGER.warning("invalid unconfirmed balance detected, rebuilding cache");
			this.rebuildCache(this.asFilter().getAll());
		}
	}

	private boolean areUnconfirmedBalancesValid() {
		return this.unconfirmedBalances.unconfirmedBalancesAreValid() && this.unconfirmedMosaicBalances.unconfirmedMosaicBalancesAreValid();
	}

	private static Iterable<Transaction> getReverseTransactions(final Collection<Transaction> transactions) {
		return () -> new ReverseListIterator<>(transactions.stream().collect(Collectors.toList()));
	}

	@Override
	public void dropExpiredTransactions(final TimeInstant time) {
		final List<Transaction> notExpiredTransactions = this.transactions.stream()
				.filter(tx -> !this.isExpired(tx, time))
				.collect(Collectors.toList());
		if (notExpiredTransactions.size() == this.transactions.size()) {
			return;
		}

		LOGGER.info("expired unconfirmed transaction in cache detected, rebuilding cache");
		this.rebuildCache(notExpiredTransactions);
	}

	private boolean isExpired(final Transaction transaction, final TimeInstant time) {
		return TransactionExtensions.streamDefault(transaction).anyMatch(t -> t.getDeadline().compareTo(time) < 0);
	}

	private void rebuildCache(final Collection<Transaction> transactions) {
		this.transactions.clear();
		this.unconfirmedBalances.clearCache();
		this.unconfirmedMosaicBalances.clearCache();

		// don't add as batch since this would fail fast and we want to keep as many transactions as possible.
		transactions.stream().forEach(this::addNew);
	}

	@Override
	public UnconfirmedTransactionsFilter asFilter() {
		return this.transactionsFilter;
	}
}
