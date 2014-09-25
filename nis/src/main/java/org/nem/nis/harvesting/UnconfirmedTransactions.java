package org.nem.nis.harvesting;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
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
	private final ConcurrentMap<Account, Amount> unconfirmedBalances = new ConcurrentHashMap<>();
	private final TransactionObserver transferObserver = new TransferObserverToTransactionObserverAdapter(
			new UnconfirmedTransactionsTransferObserver(this.unconfirmedBalances));
	private final TransactionValidator validator;

	/**
	 * Options for adding a transaction.
	 */
	public enum AddOptions {
		/**
		 * Neutral transactions should be added.
		 */
		AllowNeutral,

		/**
		 * Neutral transactions should be rejected.
		 */
		RejectNeutral
	}

	/**
	 * Creates a new unconfirmed transactions collection.
	 *
	 * @param validator The transaction validator to use.
	 */
	public UnconfirmedTransactions(final TransactionValidator validator) {
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();
		builder.add(validator);
		builder.add(new NonConflictingImportanceTransferTransactionValidator(() -> this.transactions.values()));
		this.validator = builder.build();
	}

	private UnconfirmedTransactions(
			final List<Transaction> transactions,
			final TransactionValidator validator) {
		this.validator = validator;
		for (final Transaction transaction : transactions) {
			// this constructor should compare all transactions against *confirmed* balance
			this.add(transaction, AddOptions.AllowNeutral, false);
		}
	}

	/**
	 * Gets the number of unconfirmed transactions.
	 *
	 * @return The number of unconfirmed transactions.
	 */
	public int size() {
		return this.transactions.size();
	}

	/**
	 * Adds an unconfirmed transaction.
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	public ValidationResult add(final Transaction transaction) {
		return this.add(transaction, AddOptions.RejectNeutral);
	}

	/**
	 * Adds an unconfirmed transaction.
	 *
	 * @param transaction The transaction.
	 * @param options The add options.
	 * @return true if the transaction was added.
	 */
	public ValidationResult add(final Transaction transaction, final AddOptions options) {
		return this.add(transaction, options, true);
	}

	private ValidationResult add(final Transaction transaction, final AddOptions options, final boolean execute) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		if (this.transactions.containsKey(transactionHash) || null != this.pendingTransactions.putIfAbsent(transactionHash, true)) {
			return ValidationResult.NEUTRAL;
		}

		try {
			return addInternal(transaction, transactionHash, options, execute);
		} finally {
			this.pendingTransactions.remove(transactionHash);
		}
	}

	private ValidationResult addInternal(
			final Transaction transaction,
			final Hash transactionHash,
			final AddOptions options,
			final boolean execute) {
		final ValidationResult validationResult = this.validate(transaction);
		if (!isSuccess(validationResult, options)) {
			LOGGER.warning(String.format("Transaction from %s rejected (%s)", transaction.getSigner().getAddress(), validationResult));
			return validationResult;
		}

		if (execute) {
			transaction.execute(this.transferObserver);
		}

		// TODO 20140922 J-G above the transaction result is NEUTRAL but here it is FAILURE_HASH_EXISTS
		// TODO should (1) HASH_EXISTS be neutral status or should we use NEUTRAL here too?
		// TODO 20140923 G-J I think there might be something wrong with this method. Basically, the only
		// way, that check below would return FAILURE_HASH_EXISTS, is that if there was parallel add(),
		// which succeeded first, but we probably wouldn't like .execute() above to be called twice for the same TX.
		// OTOH making this method synchronized is rather poor idea, as most of the code could run in parallel
		// (i.e. for different hashes) Should we try to solve it, and if so any idea how?
		// maybe we should add hash to this.transactions on the top of the func, and here only replace it?
		// review!
		// TODO 20140924 J-G should be fixed now
		this.transactions.put(transactionHash, transaction);
		return ValidationResult.SUCCESS;
	}

	private static boolean isSuccess(final ValidationResult result, final AddOptions options) {
		return (AddOptions.AllowNeutral == options && ValidationResult.NEUTRAL == result) || result.isSuccess();
	}

	private ValidationResult validate(final Transaction transaction) {
		return this.validator.validate(
				transaction,
				new ValidationContext((account, amount) -> this.unconfirmedBalances.get(account).compareTo(amount) >= 0));
	}

	/**
	 * Removes the specified transaction from the list of unconfirmed transactions.
	 *
	 * @param transaction The transaction to remove.
	 * @return true if the transaction was found and removed; false if the transaction was not found.
	 */
	public boolean remove(final Transaction transaction) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		if (!this.transactions.containsKey(transactionHash)) {
			return false;
		}

		transaction.undo(this.transferObserver);

		this.transactions.remove(transactionHash);
		return true;
	}

	/**
	 * Removes all transactions in the specified block.
	 *
	 * @param block The block.
	 */
	public void removeAll(final Block block) {
		for (final Transaction transaction : block.getTransactions()) {
			final Hash transactionHash = HashUtils.calculateHash(transaction);

			// don't call this.remove because transactions should not be removed when undone;
			// otherwise, the unconfirmed balances would not be correct
			this.transactions.remove(transactionHash);
		}
	}

	/**
	 * Gets all transactions.
	 *
	 * @return All transaction from this unconfirmed transactions.
	 */
	public List<Transaction> getAll() {
		final List<Transaction> transactions = this.transactions.values().stream()
				.collect(Collectors.toList());

		return this.sortTransactions(transactions);
	}

	/**
	 * Gets all transactions before the specified time. Returned list is sorted.
	 *
	 * @param time The specified time.
	 * @return The sorted list of all transactions before the specified time.
	 */
	public List<Transaction> getTransactionsBefore(final TimeInstant time) {
		final List<Transaction> transactions = this.transactions.values().stream()
				.filter(tx -> tx.getTimeStamp().compareTo(time) < 0)
				.collect(Collectors.toList());

		return this.sortTransactions(transactions);
	}

	private List<Transaction> sortTransactions(final List<Transaction> transactions) {
		Collections.sort(transactions, (lhs, rhs) -> -1 * lhs.compareTo(rhs));
		return transactions;
	}

	/**
	 * Executes all transactions.
	 */
	private void executeAll() {
		this.getAll().stream()
				.forEach(tx -> tx.execute(this.transferObserver));
	}

	// TODO 20140924 J-J review tests for this function
	/**
	 * There might be conflicting transactions on the list of unconfirmed transactions.
	 * This method iterates over *sorted* list of unconfirmed transactions, filtering out any conflicting ones.
	 * Currently conflicting transactions are NOT removed from main list of unconfirmed transactions.
	 *
	 * @param unconfirmedTransactions sorted list of unconfirmed transactions.
	 * @return filtered out list of unconfirmed transactions.
	// */
	//public List<Transaction> removeConflictingTransactions(final List<Transaction> unconfirmedTransactions) {
	//	// UnconfirmedTransactions CAN contain conflicting TXes:
	//	// a) we need to use unconfirmed balance to avoid some stupid situations (and spamming).
	//	// b) B has 0 balance, A->B 10nems, B->X 5nems with 2nem fee, since we check unconfirmed balance,
	//	//    both this TXes will get added, when creating a block, TXes are sorted by FEE,
	//	//    so B's TX will get on list before A's, and ofc it is invalid, and must get removed
	//	// c) we're leaving it in unconfirmedTxes, so it should be included in next block
	//	// TODO-CR 20140922 J-G: it would be great to add a test for this exact case ;)
	//	final UnconfirmedTransactions filteredTxes = new UnconfirmedTransactions(this.validator);
	//
	//	unconfirmedTransactions.stream()
	//			.forEach(tx -> filteredTxes.add(tx, AddOptions.AllowNeutral, false));
	//
	//	filteredTxes.executeAll();
	//	return filteredTxes.getAll();
	//}

	/**
	 * Drops transactions that have already expired.
	 *
	 * @param time The current time.
	 */
	public void dropExpiredTransactions(final TimeInstant time) {
		this.transactions.values().stream()
				.filter(tx -> tx.getDeadline().compareTo(time) < 0)
				.forEach(tx -> this.remove(tx));
	}


	//public List<Transaction> getUnconfirmedTransactionsForNewBlock(final TimeInstant blockTime) {
	//	return this.unconfirmedTransactions.removeConflictingTransactions(
	//			this.unconfirmedTransactions.getTransactionsBefore(blockTime)
	//	);
	//}
	//
	//public List<Transaction> getUnconfirmedTransactionsForNewBlock(final TimeInstant blockTime, final BlockHeight blockHeight) {
	//	// TODO-CR 20140922 G-J: want ImportanceTransferValidator: yes it should probably be passed down the chain, to be passed as predicate to add()
	//	return this.unconfirmedTransactions.removeConflictingTransactions(
	//			this.removeConflictingImportanceTransactions(
	//					blockHeight,
	//					this.unconfirmedTransactions.getTransactionsBefore(blockTime)
	//			)
	//	);
	//}
	//
	//
	//
	///**
	// * This method is for GUI's usage.
	// * Right now it returns only outgoing TXes, TODO: should it return incoming too?
	// *
	// * @param address - sender of transactions.
	// * @return The list of transactions.
	// */
	//public List<Transaction> getUnconfirmedTransactions(final Address address) {
	//	return this.unconfirmedTransactions.getTransactionsBefore(NisMain.TIME_PROVIDER.getCurrentTime()).stream()
	//			.filter(tx -> this.matchAddress(tx, address))
	//			.collect(Collectors.toList());
	//}

	/**
	 * Gets all transactions for the specified account.
	 *
	 * @param address The account address.
	 * @param time The current time.
	 * @return The filtered list of transactions.
	 */
	public UnconfirmedTransactions getTransactionsForAccount(final Address address, final TimeInstant time) {
			return new UnconfirmedTransactions(
					this.getTransactionsBefore(time).stream()
							.filter(tx -> matchAddress(tx, address))
							.collect(Collectors.toList()),
					this.validator);
	}

	private static boolean matchAddress(final Transaction transaction, final Address address) {
		if (transaction.getSigner().getAddress().equals(address)) {
			return true;
		}

		switch (transaction.getType()) {
			case TransactionTypes.TRANSFER:
				return ((TransferTransaction)transaction).getRecipient().getAddress().equals(address);

			default:
				return false;
		}
	}

	/**
	 * Gets all the unconfirmed transactions that are eligible for inclusion into the next block.
	 *
	 * @param harvester The harvester's account.
	 * @param blockTime The block time.
	 * @return The filtered list of transactions.
	 */
	public UnconfirmedTransactions getTransactionsForNewBlock(
			final Account harvester,
			final TimeInstant blockTime) {
		// in order for a transaction to be eligible for inclusion in a block, it must
		// (1) occur at or after the block time
		// (2) be signed by an account other than the harvester
		// (3) pass validation against the *confirmed* balance
		return new UnconfirmedTransactions(
				this.getTransactionsBefore(blockTime).stream()
						.filter(tx -> !tx.getSigner().equals(harvester))
						.collect(Collectors.toList()),
				this.validator);
	}

	private static class UnconfirmedTransactionsTransferObserver implements TransferObserver {
		private final Map<Account, Amount> unconfirmedBalances;

		public UnconfirmedTransactionsTransferObserver(final ConcurrentMap<Account, Amount> unconfirmedBalances) {
			this.unconfirmedBalances = unconfirmedBalances;
		}

		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
			this.notifyDebit(sender, amount);
			this.notifyCredit(recipient, amount);
		}

		@Override
		public void notifyCredit(final Account account, final Amount amount) {
			this.addToCache(account);
			final Amount newBalance = this.unconfirmedBalances.get(account).add(amount);
			this.unconfirmedBalances.replace(account, newBalance);
		}

		@Override
		public void notifyDebit(final Account account, final Amount amount) {
			this.addToCache(account);
			final Amount newBalance = this.unconfirmedBalances.get(account).subtract(amount);
			this.unconfirmedBalances.replace(account, newBalance);
		}

		private void addToCache(final Account account) {
			// it's ok to put reference here, thanks to Account being non-mutable
			this.unconfirmedBalances.putIfAbsent(account, account.getBalance());
		}
	}
}
