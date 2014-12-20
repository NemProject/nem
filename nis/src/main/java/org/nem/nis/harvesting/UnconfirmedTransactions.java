package org.nem.nis.harvesting;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A collection of unconfirmed transactions.
 */
public class UnconfirmedTransactions {
	private static final Logger LOGGER = Logger.getLogger(UnconfirmedTransactions.class.getName());

	private final ConcurrentMap<Hash, Transaction> transactions = new ConcurrentHashMap<>();
	private final ConcurrentMap<Hash, Boolean> pendingTransactions = new ConcurrentHashMap<>();
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
			final boolean blockCreation) {
		this.validatorFactory = validatorFactory;
		this.nisCache = nisCache;
		this.timeProvider = timeProvider;
		this.singleValidator = this.createSingleValidator(blockCreation);
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
			if (this.transactions.containsKey(transactionHash) || null != this.pendingTransactions.putIfAbsent(transactionHash, true)) {
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

		this.transactions.put(transactionHash, transaction);
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

	private SingleTransactionValidator createSingleValidator(final boolean blockCreation) {
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
		builder.add(this.validatorFactory.createSingle(this.nisCache.getAccountStateCache()));
		builder.add(new NonConflictingImportanceTransferTransactionValidator(() -> this.transactions.values()));
		builder.add(new TransactionDeadlineValidator(this.timeProvider));
		builder.add(new MultisigSignaturesPresentValidator(this.nisCache.getAccountStateCache(), blockCreation));

		// need to be the last one
		// that is correct we need this.transactions here
		builder.add(new MultisigSignatureValidator(this.nisCache.getAccountStateCache(), blockCreation, () -> this.transactions.values()));

		return new MultisigAwareSingleTransactionValidator(builder.build());
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
			if (!this.transactions.containsKey(transactionHash)) {
				return false;
			}

			transaction.undo(this.transferObserver);

			this.transactions.remove(transactionHash);
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
			// this is ugly but we cannot let an exception bubble up because NIS would need a restart.
			// The exception means we have to rebuild the cache because it is corrupt.
			// In theory it should never throw here.
			try {
				// undo in reverse order!
				for (final Transaction transaction : getReverseTransactions(block)) {
					this.remove(transaction);
				}
			} catch (final Exception e) { // TODO 20141218 J-B: can we at least catch a more specific exception? we can't really recover from every possible failure
				LOGGER.severe("exception during removal of unconfirmed transactions, rebuilding cache");
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
					.filter(tx -> matchAddress(tx, address))
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

	// TODO G-G 20141215: this needs tests! and
	private boolean filterMultisigTransactions(final Transaction transaction) {
		if (transaction.getType() != TransactionTypes.MULTISIG) {
			return true;
		}
		final MultisigTransaction multisigTransaction = (MultisigTransaction)transaction;
		final Account multisigAccount = multisigTransaction.getOtherTransaction().getSigner();
		final ReadOnlyAccountState multisigState = this.nisCache.getAccountStateCache().findStateByAddress(multisigAccount.getAddress());

		if (multisigTransaction.getOtherTransaction().getType() == TransactionTypes.MULTISIG_SIGNER_MODIFY) {
			final MultisigSignerModificationTransaction modification = (MultisigSignerModificationTransaction)multisigTransaction.getOtherTransaction();
			// TODO test if there is multisigmodification inside and if it's type is Del
			// (N-2 cosignatories "exception")
		}

		// we require N-1 signatures
		if (multisigState.getMultisigLinks().getCosignatories().size() - 1 == multisigTransaction.getCosignerSignatures().size()) {
			return true;
		}

		return false;
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
					.filter(tx -> tx.getType() != TransactionTypes.MULTISIG_SIGNATURE)
					.filter(tx -> this.filterMultisigTransactions(tx))
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

	private void rebuildCache(final List<Transaction> transactions) {
		this.transactions.clear();
		this.pendingTransactions.clear();
		this.unconfirmedBalances.clearCache();
		this.addNewBatch(transactions);
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

	// TODO 20141218 J-B: changes look pretty good, nice job; i have a few comments:
	// > it might make sense to pull UnconfirmedBalancesObserver out into its own class so we can test it more directly
	// > should definitely add tests for the cases that trigger the cache rebuild (since this is presumably the source of our bugs)
	private static class UnconfirmedBalancesObserver implements TransferObserver {
		private final ReadOnlyAccountStateCache accountStateCache;
		private final Map<Account, Amount> creditedAmounts = new ConcurrentHashMap<>();
		private final Map<Account, Amount> debitedAmounts = new ConcurrentHashMap<>();

		public UnconfirmedBalancesObserver(final ReadOnlyAccountStateCache accountStateCache) {
			this.accountStateCache = accountStateCache;
		}

		public Amount get(final Account account) {
			return this.getBalance(account).add(this.getCreditedAmount(account)).subtract(this.getDebitedAmount(account));
		}

		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
			this.notifyDebit(sender, amount);
			this.notifyCredit(recipient, amount);
		}

		@Override
		public void notifyCredit(final Account account, final Amount amount) {
			this.addToCache(account);
			final Amount newCreditedAmount = this.getCreditedAmount(account).add(amount);
			this.creditedAmounts.replace(account, newCreditedAmount);
		}

		@Override
		public void notifyDebit(final Account account, final Amount amount) {
			this.addToCache(account);
			final Amount newDebitedAmount = this.getDebitedAmount(account).add(amount);
			// should not be necessary but do it anyway as check
			this.getBalance(account).add(this.getCreditedAmount(account)).subtract(newDebitedAmount);
			this.debitedAmounts.replace(account, newDebitedAmount);
		}

		private void addToCache(final Account account) {
			// it's ok to put reference here, thanks to Account being non-mutable
			this.creditedAmounts.putIfAbsent(account, Amount.ZERO);
			this.debitedAmounts.putIfAbsent(account, Amount.ZERO);
		}

		private void clearCache() {
			this.creditedAmounts.clear();
			this.debitedAmounts.clear();
		}

		private boolean unconfirmedBalancesAreValid() {
			for (final Account account : this.creditedAmounts.keySet()) {
				if (this.getBalance(account).add(this.getCreditedAmount(account)).compareTo(this.getDebitedAmount(account)) < 0) {
					return false;
				}
			}

			return true;
		}

		private Amount getBalance(final Account account) {
			return this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo().getBalance();
		}

		private Amount getCreditedAmount(final Account account) {
			return this.creditedAmounts.getOrDefault(account, Amount.ZERO);
		}

		private Amount getDebitedAmount(final Account account) {
			return this.debitedAmounts.getOrDefault(account, Amount.ZERO);
		}
	}
}
