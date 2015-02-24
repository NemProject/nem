package org.nem.nis.harvesting;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.sync.DefaultDebitPredicate;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Provider of transactions for a new block.
 */
public class NewBlockTransactionsProvider {
	private static final Logger LOGGER = Logger.getLogger(NewBlockTransactionsProvider.class.getName());

	private final ReadOnlyNisCache nisCache;
	private final SingleTransactionValidator singleTransactionValidator;
	private final UnconfirmedTransactionsFilter unconfirmedTransactions;

	/**
	 * Creates a new transactions provider.
	 *
	 * @param nisCache The NIS cache.
	 * @param validatorFactory The validator factory.
	 * @param unconfirmedTransactions The unconfirmed transactions.
	 */
	public NewBlockTransactionsProvider(
			final ReadOnlyNisCache nisCache,
			final TransactionValidatorFactory validatorFactory,
			final UnconfirmedTransactionsFilter unconfirmedTransactions) {
		this.nisCache = nisCache;
		this.singleTransactionValidator = validatorFactory.createSingle(nisCache.getAccountStateCache());
		this.unconfirmedTransactions = unconfirmedTransactions;
	}

	public List<Transaction> getBlockTransactions(
			final Address harvesterAddress,
			final TimeInstant blockTime) {
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

		final List<Transaction> candidateTransactions = this.unconfirmedTransactions
				.getTransactionsBefore(blockTime).stream()
				.filter(tx -> !tx.getSigner().getAddress().equals(harvesterAddress))
				.filter(tx -> tx.getDeadline().compareTo(blockTime) >= 0)
				.collect(Collectors.toList());

		final int maxTransactions = BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK;
		int numTransactions = 0;
		final List<Transaction> blockTransactions = new ArrayList<>();
		for (final Transaction transaction : candidateTransactions) {
			final ValidationResult validationResult = this.validateSingle(transaction);
			if (validationResult.isSuccess()) {
				numTransactions += 1 + transaction.getChildTransactions().size();
				if (numTransactions > maxTransactions) {
					break;
				}

				blockTransactions.add(transaction);
			} else {
				final Hash transactionHash = HashUtils.calculateHash(transaction);
				LOGGER.info(String.format("transaction '%s' left out of block '%s'", transactionHash, validationResult));
			}
		}

		return blockTransactions;
	}

	private ValidationResult validateSingle(final Transaction transaction) {
		final ValidationContext validationContext = new ValidationContext(new DefaultDebitPredicate(this.nisCache.getAccountStateCache()));
		return this.singleTransactionValidator.validate(transaction, validationContext);
	}
}
