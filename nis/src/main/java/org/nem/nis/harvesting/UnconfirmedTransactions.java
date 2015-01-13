package org.nem.nis.harvesting;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.*;
import org.nem.nis.cache.*;
import org.nem.nis.secret.UnconfirmedBalancesObserver;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.*;

/**
 * A collection of unconfirmed transactions.
 */
public class UnconfirmedTransactions {
	private static final Logger LOGGER = Logger.getLogger(UnconfirmedTransactions.class.getName());

	private final ConcurrentMap<Hash, Transaction> transactions = new ConcurrentHashMap<>();
	private final ConcurrentMap<Hash, Transaction> pendingTransactions = new ConcurrentHashMap<>();
	private final ConcurrentMap<Hash, Boolean> childTransactions = new ConcurrentHashMap<>();
	private final UnconfirmedBalancesObserver unconfirmedBalances;
	private final TransactionObserver transferObserver;
	private final TransactionValidatorFactory validatorFactory;
	private final SingleTransactionValidator singleValidator;
	private final ReadOnlyNisCache nisCache;
	private final TimeProvider timeProvider;
	private final Object lock = new Object();

	private enum BalanceValidationOptions {
		/**
		 * The confirmed balance check occurs as part of the single transaction validator.
		 * This is used to exclude conflicting transactions when generating a block.
		 * This is accomplished by bypassing the execution of the UnconfirmedBalancesObserver
		 * (so that all balance validations are against the current account balances).
		 */
		ValidateAgainstConfirmedBalance,

		/**
		 * The unconfirmed balance check occurs as part of the execution of the UnconfirmedBalancesObserver.
		 * This is the default setting and is used when adding new transactions and
		 * when getting the unconfirmed transactions for an account.
		 * <br/>
		 * This improves the user experience:
		 * A user complained that if the GUI shows a balance of 1k NEM he can initiate many
		 * transactions with 800 NEM. All transactions were displayed in the GUI as unconfirmed giving the user the feeling he
		 * can spend more than he has. Furthermore, a new block included one of the transactions leaving the
		 * the other transaction still being displayed as unconfirmed in the GUI forever (until deadline was exceeded).
		 */
		ValidateAgainstUnconfirmedBalance,
	}

	/**
	 * Creates a new unconfirmed transactions collection.
	 *
	 * @param validatorFactory The transaction validator factory to use.
	 * @param nisCache The NIS cache to use.
	 */
	public UnconfirmedTransactions(
			final TransactionValidatorFactory validatorFactory,
			final ReadOnlyNisCache nisCache,
			final TimeProvider timeProvider) {
		this(
				new ArrayList<>(),
				BalanceValidationOptions.ValidateAgainstConfirmedBalance,
				validatorFactory,
				nisCache,
				timeProvider,
				false);
	}

	private UnconfirmedTransactions(
			final List<Transaction> transactions,
			final BalanceValidationOptions options,
			final TransactionValidatorFactory validatorFactory,
			final ReadOnlyNisCache nisCache,
			final TimeProvider timeProvider,
			final boolean blockVerification) {
		this.validatorFactory = validatorFactory;
		this.nisCache = nisCache;
		this.timeProvider = timeProvider;
		this.singleValidator = this.createSingleValidator(blockVerification);
		this.unconfirmedBalances = new UnconfirmedBalancesObserver(nisCache.getAccountStateCache());
		this.transferObserver = new TransferObserverToTransactionObserverAdapter(this.unconfirmedBalances);
		for (final Transaction transaction : transactions) {
			this.add(transaction, options == BalanceValidationOptions.ValidateAgainstUnconfirmedBalance);
		}
	}

	private UnconfirmedTransactions filter(
			final List<Transaction> transactions,
			final BalanceValidationOptions options) {
		synchronized (this.lock) {
			return new UnconfirmedTransactions(
					transactions,
					options,
					this.validatorFactory,
					this.nisCache,
					this.timeProvider,
					true);
		}
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
	 * Adds new unconfirmed transactions.
	 *
	 * @param transactions The collection of transactions.
	 * @return SUCCESS if all transactions were added successfully, NEUTRAL or FAILURE otherwise.
	 */
	public ValidationResult addNewBatch(final Collection<Transaction> transactions) {
		synchronized (this.lock) {
			final ValidationResult transactionValidationResult = this.validateBatch(transactions);
			if (!transactionValidationResult.isSuccess()) {
				return transactionValidationResult;
			}

			return ValidationResult.aggregate(transactions.stream().map(transaction -> this.add(transaction, true)).iterator());
		}
	}

	/**
	 * Adds a new unconfirmed transaction.
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	public ValidationResult addNew(final Transaction transaction) {
		synchronized (this.lock) {
			final ValidationResult transactionValidationResult = this.validateBatch(Arrays.asList(transaction));
			return transactionValidationResult.isSuccess()
					? this.add(transaction, true)
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
		return this.add(transaction, true);
	}

	private ValidationResult add(final Transaction transaction, final boolean execute) {
		synchronized (this.lock) {
			final Hash transactionHash = HashUtils.calculateHash(transaction);
			if (this.hasTransactionInCache(transaction, transactionHash) || null != this.pendingTransactions.putIfAbsent(transactionHash, transaction)) {
				return ValidationResult.NEUTRAL;
			}

			if (!transaction.verify()) {
				return ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE;
			}

			try {
				return this.addInternal(transaction, transactionHash, execute);
			} finally {
				this.pendingTransactions.remove(transactionHash);
			}
		}
	}

	private ValidationResult addInternal(
			final Transaction transaction,
			final Hash transactionHash,
			final boolean execute) {
		final ValidationResult validationResult = this.validateSingle(transaction);
		if (!validationResult.isSuccess()) {
			LOGGER.warning(String.format("Transaction from %s rejected (%s)", transaction.getSigner().getAddress(), validationResult));
			return validationResult;
		}

		if (execute) {
			transaction.execute(this.transferObserver);
		}

		this.addTransactionToCache(transaction, transactionHash);
		return ValidationResult.SUCCESS;
	}

	private ValidationResult validateBatch(final Collection<Transaction> transactions) {
		final TransactionsContextPair pair = new TransactionsContextPair(transactions, this.createValidationContext());
		return this.validatorFactory.createBatch(this.nisCache.getTransactionHashCache()).validate(Arrays.asList(pair));
	}

	private ValidationResult validateSingle(final Transaction transaction) {
		return this.singleValidator.validate(transaction, this.createValidationContext());
	}

	private ValidationContext createValidationContext() {
		return new ValidationContext((account, amount) -> this.getUnconfirmedBalance(account).compareTo(amount) >= 0);
	}

	private SingleTransactionValidator createSingleValidator(final boolean blockVerification) {
		final ReadOnlyAccountStateCache accountStateCache = this.nisCache.getAccountStateCache();
		final AggregateSingleTransactionValidatorBuilder builder = this.validatorFactory.createSingleBuilder(accountStateCache);
		builder.add(new NonConflictingImportanceTransferTransactionValidator(() -> this.transactions.values()));
		builder.add(new TransactionDeadlineValidator(this.timeProvider));

		// TODO 20150103 J-J: probably should add another function to the factory for blockVerification validators
		// > e.g., (that adds MultisigSignaturesPresentValidator)

		if (!blockVerification) {
			// need to be the last one
			builder.add(new MultisigSignatureValidator(accountStateCache,
					// we need pendingTransactions see UnconfirmedTransactionsMultisigTest.multisigTransactionWithSignatureIsAccepted
					// TODO 20150107 G-G,J: any nicer way to do it? maybe supplier could return stream?
					() -> Stream.concat(this.pendingTransactions.values().stream(), this.transactions.values().stream()).collect(Collectors.toList())));
		} else {
			builder.add(new MultisigSignaturesPresentValidator(accountStateCache));
		}

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
			final Hash transactionHash = HashUtils.calculateHash(transaction);
			if (!this.hasTransactionInCache(transaction, transactionHash)) {
				return false;
			}

			transaction.undo(this.transferObserver);

			this.removeTransactionFromCache(transaction, transactionHash);
			return true;
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
			if (!this.unconfirmedBalances.unconfirmedBalancesAreValid()) {
				LOGGER.warning("invalid unconfirmed balance detected, rebuilding cache");
				this.rebuildCache(this.getAll());
			}
		}
	}

	private static Iterable<Transaction> getReverseTransactions(final Block block) {
		return () -> new ReverseListIterator<>(block.getTransactions());
	}

	/**
	 * Gets all transactions.
	 *
	 * @return All transaction from this unconfirmed transactions.
	 */
	public List<Transaction> getAll() {
		synchronized (this.lock) {
			final List<Transaction> transactions = this.transactions.values().stream()
					.collect(Collectors.toList());
			return this.sortTransactions(transactions);
		}
	}

	/**
	 * Gets the transactions for which the hash short id is not in the given collection.
	 *
	 * @return The unknown transactions.
	 */
	public List<Transaction> getUnknownTransactions(final Collection<HashShortId> knownHashShortIds) {
		// probably faster to use hash map than collection
		synchronized (this.lock) {
			final HashMap<HashShortId, Transaction> unknownHashShortIds = new HashMap<>(this.transactions.size());
			this.transactions.values().stream()
					.forEach(t -> unknownHashShortIds.put(new HashShortId(HashUtils.calculateHash(t).getShortId()), t));
			knownHashShortIds.stream().forEach(unknownHashShortIds::remove);
			return unknownHashShortIds.values().stream().collect(Collectors.toList());
		}
	}

	/**
	 * Gets the most recent transactions up to a given limit.
	 *
	 * @return The most recent transactions from this unconfirmed transactions.
	 */
	public List<Transaction> getMostRecentTransactionsForAccount(final Address address, final int maxSize) {
		synchronized (this.lock) {
			return this.transactions.values().stream()
					.filter(tx -> matchAddress(tx, address) || isCosignatory(tx, address))
					.sorted((t1, t2) -> -t1.getTimeStamp().compareTo(t2.getTimeStamp()))
					.limit(maxSize)
					.collect(Collectors.toList());
		}
	}

	/**
	 * Gets all transactions up to a given limit of transactions.
	 *
	 * @return The list of unconfirmed transactions.
	 */
	public List<Transaction> getMostImportantTransactions(final int maxTransactions) {
		synchronized (this.lock) {
			return this.transactions.values().stream()
					.sorted((lhs, rhs) -> -1 * lhs.compareTo(rhs))
					.limit(maxTransactions)
					.collect(Collectors.toList());
		}
	}

	/**
	 * Gets all transactions before the specified time. Returned list is sorted.
	 *
	 * @param time The specified time.
	 * @return The sorted list of all transactions before the specified time.
	 */
	public List<Transaction> getTransactionsBefore(final TimeInstant time) {
		synchronized (this.lock) {
			final List<Transaction> transactions = this.transactions.values().stream()
					.filter(tx -> tx.getTimeStamp().compareTo(time) < 0)
							// filter out signatures because we don't want them to be directly inside a block
					.filter(tx -> tx.getType() != TransactionTypes.MULTISIG_SIGNATURE)
					.collect(Collectors.toList());

			return this.sortTransactions(transactions);
		}
	}

	private List<Transaction> sortTransactions(final List<Transaction> transactions) {
		Collections.sort(transactions, (lhs, rhs) -> -1 * lhs.compareTo(rhs));
		return transactions;
	}

	/**
	 * Drops transactions that have already expired.
	 *
	 * @param time The current time.
	 */
	public void dropExpiredTransactions(final TimeInstant time) {
		synchronized (this.lock) {
			final List<Transaction> notExpiredTransactions = this.transactions.values().stream()
					.filter(tx -> tx.getDeadline().compareTo(time) >= 0)
					.collect(Collectors.toList());
			if (notExpiredTransactions.size() == this.transactions.size()) {
				return;
			}

			LOGGER.info("expired unconfirmed transaction in cache detected, rebuilding cache");
			this.rebuildCache(notExpiredTransactions);
		}
	}

	private boolean hasTransactionInCache(final Transaction transaction, final Hash transactionHash) {
		return this.transactions.containsKey(transactionHash) ||
				this.childTransactions.containsKey(transactionHash) ||
				transaction.getChildTransactions().stream()
						.anyMatch(t -> {
							final Hash key = HashUtils.calculateHash(t);
							return this.childTransactions.containsKey(key) || this.transactions.containsKey(key);
						});
	}

	private void addTransactionToCache(final Transaction transaction, final Hash transactionHash) {
		for (final Transaction childTransaction : transaction.getChildTransactions()) {
			this.childTransactions.put(HashUtils.calculateHash(childTransaction), true);
		}
		this.transactions.put(transactionHash, transaction);
	}

	private void removeTransactionFromCache(final Transaction transaction, final Hash transactionHash) {
		for (final Transaction childTransaction : transaction.getChildTransactions()) {
			this.childTransactions.remove(HashUtils.calculateHash(childTransaction));
		}
		this.transactions.remove(transactionHash);
	}

	private void rebuildCache(final List<Transaction> transactions) {
		this.transactions.clear();
		this.pendingTransactions.clear();
		this.childTransactions.clear();
		this.unconfirmedBalances.clearCache();

		// don't add as batch since this would fail fast and we want to keep as many transactions as possible.
		transactions.stream().forEach(t -> this.addNew(t));
	}

	/**
	 * Gets all transactions for the specified account.
	 *
	 * @param address The account address.
	 * @return The filtered list of transactions.
	 */
	public UnconfirmedTransactions getTransactionsForAccount(final Address address) {
		synchronized (this.lock) {
			return this.filter(
					this.getAll().stream()
							.filter(tx -> matchAddress(tx, address))
							.collect(Collectors.toList()),
					BalanceValidationOptions.ValidateAgainstUnconfirmedBalance);
		}
	}

	private static boolean matchAddress(final Transaction transaction, final Address address) {
		return transaction.getAccounts().stream()
				.map(account -> account.getAddress())
				.anyMatch(transactionAddress -> transactionAddress.equals(address));
	}


	private boolean isCosignatory(final Transaction transaction, final Address address) {
		if (TransactionTypes.MULTISIG != transaction.getType()) {
			return false;
		}
		final ReadOnlyAccountState state = this.nisCache.getAccountStateCache().findStateByAddress(address);
		return state.getMultisigLinks().isCosignatoryOf(((MultisigTransaction)transaction).getOtherTransaction().getSigner().getAddress());
	}

	/**
	 * Gets all the unconfirmed transactions that are eligible for inclusion into the next block.
	 *
	 * @param harvesterAddress The harvester's address.
	 * @param blockTime The block time.
	 * @return The filtered list of transactions.
	 */
	public UnconfirmedTransactions getTransactionsForNewBlock(final Address harvesterAddress, final TimeInstant blockTime) {
		// in order for a transaction to be eligible for inclusion in a block, it must
		// (1) occur at or before the block time
		// (2) be signed by an account other than the harvester
		// (3) not already be expired (relative to the block time):
		// - BlockGenerator.generateNextBlock() calls dropExpiredTransactions() and later getTransactionsForNewBlock().
		// - In-between it is possible that unconfirmed transactions are polled and thus expired (relative to the block time)
		// - transactions are in our cache when we call getTransactionsForNewBlock().
		// (4) pass validation against the *confirmed* balance

		// this filter validates all transactions against confirmed balance:
		// a) we need to use unconfirmed balance to avoid some stupid situations (and spamming).
		// b) B has 0 balance, A->B 10nems, B->X 5nems with 2nem fee, since we check unconfirmed balance,
		//    both this TXes will get added, when creating a block, TXes are sorted by FEE,
		//    so B's TX will get on list before A's, and ofc it is invalid, and must get removed
		// c) we're leaving it in unconfirmedTxes, so it should be included in next block
		synchronized (this.lock) {
			return this.filter(
					this.getTransactionsBefore(blockTime).stream()
							.filter(tx -> !tx.getSigner().getAddress().equals(harvesterAddress))
							.filter(tx -> tx.getDeadline().compareTo(blockTime) >= 0)
							.collect(Collectors.toList()),
					BalanceValidationOptions.ValidateAgainstConfirmedBalance);
		}
	}
}
