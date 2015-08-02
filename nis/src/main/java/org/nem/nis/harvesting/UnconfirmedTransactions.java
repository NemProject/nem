package org.nem.nis.harvesting;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.secret.*;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.AggregateSingleTransactionValidatorBuilder;
import org.nem.nis.validators.unconfirmed.TransactionDeadlineValidator;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A collection of unconfirmed transactions.
 */
public class UnconfirmedTransactions implements UnconfirmedTransactionsFilter {
	private static final Logger LOGGER = Logger.getLogger(UnconfirmedTransactions.class.getName());

	private final UnconfirmedTransactionsCache transactions;
	private final UnconfirmedTransactionsFilter transactionsFilter;
	private final UnconfirmedBalancesObserver unconfirmedBalances;
	private final UnconfirmedMosaicBalancesObserver unconfirmedMosaicBalances;
	private final TransactionObserver transferObserver;
	private final TransactionValidatorFactory validatorFactory;
	private final SingleTransactionValidator singleValidator;
	private final TransactionSpamFilter spamFilter;
	private final ReadOnlyNisCache nisCache;
	private final TimeProvider timeProvider;
	private final Supplier<BlockHeight> blockHeightSupplier;
	private final Object lock = new Object();

	/**
	 * Creates a new unconfirmed transactions collection.
	 *
	 * @param validatorFactory The transaction validator factory to use.
	 * @param nisCache The NIS cache to use.
	 * @param timeProvider The time provider.
	 * @param blockHeightSupplier Supplier that provides the current block height.
	 */
	public UnconfirmedTransactions(
			final TransactionValidatorFactory validatorFactory,
			final ReadOnlyNisCache nisCache,
			final TimeProvider timeProvider,
			final Supplier<BlockHeight> blockHeightSupplier) {
		this.validatorFactory = validatorFactory;
		this.nisCache = nisCache;
		this.timeProvider = timeProvider;
		this.blockHeightSupplier = blockHeightSupplier;
		this.singleValidator = this.createSingleValidator();
		this.unconfirmedBalances = new UnconfirmedBalancesObserver(nisCache.getAccountStateCache());
		this.unconfirmedMosaicBalances = new UnconfirmedMosaicBalancesObserver(nisCache.getNamespaceCache());
		this.transferObserver = this.createObserver();

		final MultisigSignatureMatchPredicate matchPredicate = new MultisigSignatureMatchPredicate(this.nisCache.getAccountStateCache());
		this.transactions = new UnconfirmedTransactionsCache(this::verifyAndValidate, matchPredicate::isMatch);
		this.transactionsFilter = new DefaultUnconfirmedTransactionsFilter(
				this.transactions,
				new ImpactfulTransactionPredicate(this.nisCache.getAccountStateCache()));
		this.spamFilter = new TransactionSpamFilter(this.nisCache, this.transactions);
	}

	/**
	 * Gets the number of unconfirmed transactions.
	 *
	 * @return The number of unconfirmed transactions.
	 */
	public int size() {
		synchronized (this.lock) {
			return this.transactions.size();
		}
	}

	/**
	 * Gets the unconfirmed balance for the specified account.
	 *
	 * @param account The account.
	 * @return The unconfirmed balance.
	 */
	public Amount getUnconfirmedBalance(final Account account) {
		synchronized (this.lock) {
			return this.unconfirmedBalances.get(account);
		}
	}

	/**
	 * Gets the unconfirmed mosaic balance for the specified account and mosaic id.
	 *
	 * @param account The account.
	 * @param mosaicId The mosaic id.
	 * @return The unconfirmed mosaic balance.
	 */
	public Quantity getUnconfirmedMosaicBalance(final Account account, final MosaicId mosaicId) {
		synchronized (this.lock) {
			return this.unconfirmedMosaicBalances.get(account, mosaicId);
		}
	}

	private TransactionObserver createObserver() {
		final AggregateTransactionObserverBuilder builder = new AggregateTransactionObserverBuilder();
		builder.add(this.unconfirmedBalances);
		builder.add(this.unconfirmedMosaicBalances);
		return builder.build();
	}

	/**
	 * Adds new unconfirmed transactions.
	 *
	 * @param transactions The collection of transactions.
	 * @return SUCCESS if all transactions were added successfully, NEUTRAL or FAILURE otherwise.
	 */
	public ValidationResult addNewBatch(final Collection<Transaction> transactions) {
		synchronized (this.lock) {
			final Collection<Transaction> filteredTransactions = this.spamFilter.filter(transactions);
			final ValidationResult transactionValidationResult = this.validateBatch(filteredTransactions);
			if (!transactionValidationResult.isSuccess()) {
				return transactionValidationResult;
			}

			return ValidationResult.aggregateNoShortCircuit(filteredTransactions.stream().map(this::add).iterator());
		}
	}

	/**
	 * Adds a new unconfirmed transaction.
	 *
	 * @param transaction The transaction.
	 * @return The result of transaction validation.
	 */
	public ValidationResult addNew(final Transaction transaction) {
		synchronized (this.lock) {
			// check is needed to distinguish between NEUTRAL and FAILURE_TRANSACTION_CACHE_TOO_FULL
			if (this.transactions.contains(transaction)) {
				return ValidationResult.NEUTRAL;
			}

			final Collection<Transaction> filteredTransactions = this.spamFilter.filter(Collections.singletonList(transaction));
			if (filteredTransactions.isEmpty()) {
				return ValidationResult.FAILURE_TRANSACTION_CACHE_TOO_FULL;
			}

			final ValidationResult transactionValidationResult = this.validateBatch(filteredTransactions);
			return transactionValidationResult.isSuccess()
					? this.add(transaction)
					: transactionValidationResult;
		}
	}

	/**
	 * Adds an unconfirmed transaction that has been added previously (so some validation checks can be skipped).
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	public ValidationResult addExisting(final Transaction transaction) {
		return this.add(transaction);
	}

	private ValidationResult verifyAndValidate(final Transaction transaction) {
		if (!transaction.verify()) {
			return ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE;
		}

		final ValidationResult validationResult = this.validateSingle(transaction);
		if (!validationResult.isSuccess()) {
			LOGGER.warning(String.format("Transaction from %s rejected (%s)", transaction.getSigner().getAddress(), validationResult));
			return validationResult;
		}

		return ValidationResult.SUCCESS;
	}

	private ValidationResult add(final Transaction transaction) {
		synchronized (this.lock) {
			final ValidationResult validationResult = this.transactions.add(transaction);
			if (validationResult.isSuccess()) {
				transaction.execute(this.transferObserver);
			}

			return validationResult;
		}
	}

	private ValidationResult validateBatch(final Collection<Transaction> transactions) {
		final TransactionsContextPair pair = new TransactionsContextPair(transactions, this.createValidationContext());
		return this.validatorFactory.createBatch(this.nisCache.getTransactionHashCache()).validate(Collections.singletonList(pair));
	}

	private ValidationResult validateSingle(final Transaction transaction) {
		return this.singleValidator.validate(transaction, this.createValidationContext());
	}

	private ValidationContext createValidationContext() {
		final BlockHeight currentHeight = this.blockHeightSupplier.get();
		final ValidationState validationState = new ValidationState(
				(account, amount) -> this.getUnconfirmedBalance(account).compareTo(amount) >= 0,
				(account, mosaic) -> this.getUnconfirmedMosaicBalance(account, mosaic.getMosaicId()).compareTo(mosaic.getQuantity()) >= 0);
		return new ValidationContext(
				currentHeight.next(),
				currentHeight,
				validationState);
	}

	private SingleTransactionValidator createSingleValidator() {
		final AggregateSingleTransactionValidatorBuilder builder = this.validatorFactory.createIncompleteSingleBuilder(this.nisCache);
		builder.add(new TransactionDeadlineValidator(this.timeProvider));
		return builder.build();
	}

	/**
	 * Removes the specified transaction from the list of unconfirmed transactions.
	 *
	 * @param transaction The transaction to remove.
	 * @return true if the transaction was found and removed; false if the transaction was not found.
	 */
	public boolean remove(final Transaction transaction) {
		synchronized (this.lock) {
			final boolean isRemoved = this.transactions.remove(transaction);
			if (isRemoved) {
				transaction.undo(this.transferObserver);
			}

			return isRemoved;
		}
	}

	/**
	 * Removes all transactions in the specified block.
	 *
	 * @param block The block.
	 */
	public void removeAll(final Block block) {
		synchronized (this.lock) {
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
				for (final Transaction transaction : getReverseTransactions(block)) {
					this.remove(transaction);
				}
			} catch (final NegativeBalanceException e) {
				LOGGER.severe("illegal argument exception during removal of unconfirmed transactions, rebuilding cache");
				this.rebuildCache(this.getAll());
				return;
			}

			// Consider the following scenario:
			// Account A has 10 NEM and sends tx 1 (A -> B 8 NEM) to node 1 and tx2 (A -> B 9 NEM) to node 2.
			// UnconfirmedBalancesObserver of node 1 sees for A: balance 10, credited 0, debited 8 -> unconfirmed balance is 2
			// Now node 2 harvests a block and includes tx2. node 1 receives + executes the block with tx2.
			// UnconfirmedBalancesObserver of node 1 sees for A: balance 1, credited 0, debited 8 -> unconfirmed balance is -7
			// Next call to UnconfirmedBalancesObserver.get(A) results in an exception.
			// This means a new block can ruin the unconfirmed balance. We have to check if all balances are still valid.
			if (!this.unconfirmedBalances.unconfirmedBalancesAreValid() ||
				!this.unconfirmedMosaicBalances.unconfirmedMosaicBalancesAreValid()) {
				LOGGER.warning("invalid unconfirmed balance detected, rebuilding cache");
				this.rebuildCache(this.getAll());
			}
		}
	}

	private static Iterable<Transaction> getReverseTransactions(final Block block) {
		return () -> new ReverseListIterator<>(block.getTransactions());
	}

	//region UnconfirmedTransactionsFilter

	@Override
	public List<Transaction> getAll() {
		synchronized (this.lock) {
			return this.transactionsFilter.getAll();
		}
	}

	@Override
	public List<Transaction> getUnknownTransactions(final Collection<HashShortId> knownHashShortIds) {
		synchronized (this.lock) {
			return this.transactionsFilter.getUnknownTransactions(knownHashShortIds);
		}
	}

	@Override
	public List<Transaction> getMostRecentTransactionsForAccount(final Address address, final int maxTransactions) {
		synchronized (this.lock) {
			return this.transactionsFilter.getMostRecentTransactionsForAccount(address, maxTransactions);
		}
	}

	@Override
	public List<Transaction> getTransactionsBefore(final TimeInstant time) {
		synchronized (this.lock) {
			return this.transactionsFilter.getTransactionsBefore(time);
		}
	}

	//endregion

	/**
	 * Drops transactions that have already expired.
	 *
	 * @param time The current time.
	 */
	public void dropExpiredTransactions(final TimeInstant time) {
		synchronized (this.lock) {
			final List<Transaction> notExpiredTransactions = this.transactions.stream()
					.filter(tx -> !this.isExpired(tx, time))
					.collect(Collectors.toList());
			if (notExpiredTransactions.size() == this.transactions.size()) {
				return;
			}

			LOGGER.info("expired unconfirmed transaction in cache detected, rebuilding cache");
			this.rebuildCache(notExpiredTransactions);
		}
	}

	private boolean isExpired(final Transaction transaction, final TimeInstant time) {
		return TransactionExtensions.streamDefault(transaction).anyMatch(t -> t.getDeadline().compareTo(time) < 0);
	}

	private void rebuildCache(final List<Transaction> transactions) {
		this.transactions.clear();
		this.unconfirmedBalances.clearCache();
		this.unconfirmedMosaicBalances.clearCache();

		// don't add as batch since this would fail fast and we want to keep as many transactions as possible.
		transactions.stream().forEach(this::addNew);
	}
}
