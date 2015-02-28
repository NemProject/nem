package org.nem.nis.harvesting;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.cache.*;
import org.nem.nis.chain.*;
import org.nem.nis.secret.*;
import org.nem.nis.sync.DefaultDebitPredicate;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
/**
 * TODO 20150224 J-B,G: this change has the following implications -
 * > (1) elimination of non-conflicting validators
 *       (a) These validators were added in order to prevent multiple transactions from being added that could
 *           not both be in the next block; without these validators, unconfirmed transactions (UT) could create
 *           a block that would fail block validation in BlockChainServices (BCS)
 *       (b) Since their main purpose was to eliminate the production of an invalid block by UT, they could be
 *           eliminated by running block validation (at least the block validators that inspect transaction conflicts)
 *           when next block transactions are selected
 *       (c) [FUNCTIONAL CHANGE] The proposed implementation does not prevent the addition of invalid single block
 *           transactions; for example, multiple aggregate modifications for the same account can be added without
 *           error to UT; the block filtering is only done when transactions are retrieved from UT
 * > (2) elimination of block validators that perform in-block checks where a corresponding transaction validator already exists
 *       (a) These block validators were added to ensure that there couldn't be conflicting transactions for an account
 *           undergoing a change (e.g. making an importance transfer and a transfer from the same account).
 *       (b) [FUNCTIONAL CHANGE] These block validators can be eliminated by forcing the execution of transactions
 *           individually instead of in block batches. Each transaction can check the current state after being modified
 *           by all previous transactions.
 *       (c) [FUNCTIONAL CHANGE] one difference is that the block validators didn't care about transaction ordering;
 *           for example a transfer and importance transfer for the same account would be blocked; in contrast, the
 *           proposed change does care about order; so, a transfer followed by an importance transfer is allowed,
 *           whereas the reverse isn't (basically, the unit of execution is shrunk from block to transaction
 *           and ordering of cross-block transactions was always important).
 * > (3) Please review and let me know if we should continue with this change or not. It is a little risky at this stage,
 *       but, hopefully it will eliminate bugs around inconsistent validators. Especially, pay attention to test changes.
 *       Also note that these changes can result in blocks being produced that are not accepted by currently running code
 *       (due to changes in the execution unit).
 *
 * TODO 20150224 J-J: need to expand on tests for this class
 * TODO 20150224 J-J: need to add fork
 */

/**
 * Provider of transactions for a new block.
 */
public class DefaultNewBlockTransactionsProvider implements NewBlockTransactionsProvider {
	private static final Logger LOGGER = Logger.getLogger(DefaultNewBlockTransactionsProvider.class.getName());

	private final ReadOnlyNisCache nisCache;
	private final TransactionValidatorFactory validatorFactory;
	private final BlockValidatorFactory blockValidatorFactory;
	private final BlockTransactionObserverFactory observerFactory;
	private final UnconfirmedTransactionsFilter unconfirmedTransactions;

	/**
	 * Creates a new transactions provider.
	 *
	 * @param nisCache The NIS cache.
	 * @param validatorFactory The validator factory.
	 * @param blockValidatorFactory The block validator factory.
	 * @param observerFactory The observer factory.
	 * @param unconfirmedTransactions The unconfirmed transactions.
	 */
	public DefaultNewBlockTransactionsProvider(
			final ReadOnlyNisCache nisCache,
			final TransactionValidatorFactory validatorFactory,
			final BlockValidatorFactory blockValidatorFactory,
			final BlockTransactionObserverFactory observerFactory,
			final UnconfirmedTransactionsFilter unconfirmedTransactions) {
		this.nisCache = nisCache;
		this.validatorFactory = validatorFactory;
		this.blockValidatorFactory = blockValidatorFactory;
		this.observerFactory = observerFactory;
		this.unconfirmedTransactions = unconfirmedTransactions;
	}

	@Override
	public List<Transaction> getBlockTransactions(final Address harvesterAddress, final TimeInstant blockTime, final BlockHeight blockHeight) {
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

		// this is used as a way to run block validation on unconfirmed transactions
		final Block tempBlock = new Block(
				new Account(new KeyPair()),
				Hash.ZERO,
				Hash.ZERO,
				blockTime,
				blockHeight);

		final NisCache nisCache = this.nisCache.copy();
		final BlockValidator blockValidator = this.blockValidatorFactory.createTransactionOnly(nisCache);
		final SingleTransactionValidator transactionValidator = this.validatorFactory.createSingle(nisCache.getAccountStateCache());
		final BlockTransactionObserver observer = this.observerFactory.createExecuteCommitObserver(nisCache);
		final BlockProcessor processor = new BlockExecuteProcessor(nisCache, tempBlock, observer);

		for (final Transaction transaction : candidateTransactions) {
			final ValidationContext validationContext = new ValidationContext(new DefaultDebitPredicate(this.nisCache.getAccountStateCache()));
			final ValidationResult validationResult = transactionValidator.validate(transaction, validationContext);
			if (validationResult.isSuccess()) {
				tempBlock.addTransaction(transaction);
				if (!blockValidator.validate(tempBlock).isSuccess()) {
					tempBlock.getTransactions().remove(tempBlock.getTransactions().size() - 1);
					continue;
				}

				processor.process(transaction);

				numTransactions += 1 + transaction.getChildTransactions().size();
				if (numTransactions > maxTransactions) {
					tempBlock.getTransactions().remove(tempBlock.getTransactions().size() - 1);
					break;
				}
			} else {
				final Hash transactionHash = HashUtils.calculateHash(transaction);
				LOGGER.info(String.format("transaction '%s' left out of block '%s'", transactionHash, validationResult));
			}
		}

		return tempBlock.getTransactions();
	}
}
