package org.nem.nis;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dao.*;
import org.nem.nis.harvesting.UnlockedAccounts;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.poi.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.validators.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

//
// Initial logic is as follows:
//   * we receive new TX, IF it hasn't been seen,
//     it is added to unconfirmedTransactions,
//   * blockGeneratorExecutor periodically tries to generate a block containing
//     unconfirmed transactions
//   * if it succeeded, block is added to the db and propagated to the network
//
// fork resolution should solve the rest
//
public class Foraging {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());
	private static final int TRANSACTION_MAX_ALLOWED_TIME_DEVIATION = 30;

	private final UnlockedAccounts unlockedAccounts;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final AccountLookup accountLookup;
	private final PoiFacade poiFacade;
	private final BlockDao blockDao;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;

	@Autowired(required = true)
	public Foraging(
			final AccountLookup accountLookup,
			final PoiFacade poiFacade,
			final BlockDao blockDao,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final TransactionValidatorFactory validatorFactory,
			final UnlockedAccounts unlockedAccounts) {
		this.accountLookup = accountLookup;
		this.poiFacade = poiFacade;
		this.blockDao = blockDao;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.unlockedAccounts = unlockedAccounts;

		this.unconfirmedTransactions = new UnconfirmedTransactions(validatorFactory.create(this.poiFacade));
	}

	/**
	 * Gets the number of unconfirmed transactions.
	 *
	 * @return The number of unconfirmed transactions.
	 */
	public int getNumUnconfirmedTransactions() {
		return this.unconfirmedTransactions.size();
	}

	/**
	 * Adds transaction to list of unconfirmed transactions.
	 *
	 * @param transaction transaction that isValid() and verify()-ed
	 * @return false if given transaction has already been seen, true if it has been added
	 */
	public ValidationResult addUnconfirmedTransactionWithoutDbCheck(final Transaction transaction) {
		return this.unconfirmedTransactions.addValidOrNeutral(transaction);
	}

	private ValidationResult addUnconfirmedTransaction(final Transaction transaction) {
		return this.unconfirmedTransactions.addValid(transaction);
	}

	/**
	 * Removes all block's transactions from list of unconfirmed transactions
	 *
	 * @param block Block containing TXes that are to be removed.
	 */
	public void removeFromUnconfirmedTransactions(final Block block) {
		this.unconfirmedTransactions.removeAll(block);
	}

	/**
	 * Checks if transaction fits in time limit window, if so add it to
	 * list of unconfirmed transactions.
	 *
	 * @param transaction - transaction that isValid() and verify()-ed
	 * @return NEUTRAL if given transaction has already been seen or isn't within the time window, SUCCESS if it has been added
	 */
	public ValidationResult processTransaction(final Transaction transaction) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		// rest is checked by isValid()
		if (transaction.getTimeStamp().compareTo(currentTime.addSeconds(TRANSACTION_MAX_ALLOWED_TIME_DEVIATION)) > 0) {
			return ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE;
		}
		if (transaction.getTimeStamp().compareTo(currentTime.addSeconds(-TRANSACTION_MAX_ALLOWED_TIME_DEVIATION)) < 0) {
			return ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_PAST;
		}

		return this.addUnconfirmedTransaction(transaction);
	}

	/**
	 * Processes every transaction in the list.
	 * Since this method is called in the synchronization process, it doesn't make sense to return a value.
	 *
	 * @param transactions The transactions.
	 */
	public void processTransactions(final Collection<Transaction> transactions) {
		transactions.stream().forEach(tx -> this.processTransaction(tx));
	}

	private boolean matchAddress(final Transaction transaction, final Address address) {
		// @formatter:off
		return (transaction.getSigner().getAddress().equals(address) ||
				(transaction.getType() == TransactionTypes.TRANSFER &&
				((TransferTransaction)transaction).getRecipient().getAddress().equals(address)));
		// @formatter:on
	}

	/**
	 * This method is for GUI's usage.
	 * Right now it returns only outgoing TXes, TODO: should it return incoming too?
	 *
	 * @param address - sender of transactions.
	 * @return The list of transactions.
	 */
	public List<Transaction> getUnconfirmedTransactions(final Address address) {
		return this.unconfirmedTransactions.getTransactionsBefore(NisMain.TIME_PROVIDER.getCurrentTime()).stream()
				.filter(tx -> this.matchAddress(tx, address))
				.collect(Collectors.toList());
	}

	private boolean checkImportance(final Transaction transaction, final BlockHeight height) {
		// TODO BUG TODO
		//if (transaction.getType() == TransactionTypes.IMPORTANCE_TRANSFER) {
		//	return BlockChainValidator.verifyImportanceTransfer(this.poiFacade, height, (ImportanceTransferTransaction)transaction);
		//}
		return true;
	}

	private List<Transaction> removeConflictingImportanceTransactions(final BlockHeight height, final List<Transaction> transactions) {
		if (height == null) {
			return transactions;
		}
		return transactions.stream()
				.filter(tx -> this.checkImportance(tx, height))
				.collect(Collectors.toList());
	}

	public List<Transaction> getUnconfirmedTransactionsForNewBlock(final TimeInstant blockTime) {
		return this.unconfirmedTransactions.removeConflictingTransactions(
				this.unconfirmedTransactions.getTransactionsBefore(blockTime)
		);
	}

	public List<Transaction> getUnconfirmedTransactionsForNewBlock(final TimeInstant blockTime, final BlockHeight blockHeight) {
		// TODO-CR 20140922 G-J: want ImportanceTransferValidator: yes it should probably be passed down the chain, to be passed as predicate to add()
		return this.unconfirmedTransactions.removeConflictingTransactions(
				this.removeConflictingImportanceTransactions(
						blockHeight,
						this.unconfirmedTransactions.getTransactionsBefore(blockTime)
				)
		);
	}

	/**
	 * Filter out any transaction that has the harvester as sender.
	 *
	 * @param transactions The original list of transactions.
	 * @param harvester The harvester's account.
	 * @return The filtered list of transactions.
	 */
	public List<Transaction> filterTransactionsForHarvester(final Collection<Transaction> transactions, final Account harvester) {
		return transactions.stream()
				.filter(tx -> !tx.getSigner().equals(harvester))
				.collect(Collectors.toList());
	}

	/**
	 * Returns foraged block or null.
	 *
	 * @return Best block that could be created by unlocked accounts.
	 */
	public Block forageBlock(final BlockScorer blockScorer) {
		if (this.blockChainLastBlockLayer.getLastDbBlock() == null || this.unlockedAccounts.size() == 0) {
			return null;
		}

		LOGGER.fine("block generation " + Integer.toString(this.unconfirmedTransactions.size()) + " " + Integer.toString(this.unlockedAccounts.size()));

		Block bestBlock = null;
		long bestScore = Long.MIN_VALUE;
		// because of access to unconfirmedTransactions, and lastBlock*

		final TimeInstant blockTime = NisMain.TIME_PROVIDER.getCurrentTime();
		this.unconfirmedTransactions.dropExpiredTransactions(blockTime);

		try {
			synchronized (this.blockChainLastBlockLayer) {
				final org.nem.nis.dbmodel.Block dbLastBlock = this.blockChainLastBlockLayer.getLastDbBlock();
				final Collection<Transaction> transactionList = this.getUnconfirmedTransactionsForNewBlock(blockTime, new BlockHeight(dbLastBlock.getHeight()));
				final Block lastBlock = BlockMapper.toModel(dbLastBlock, this.accountLookup);
				final BlockDifficulty difficulty = this.calculateDifficulty(blockScorer, lastBlock);

				// TODO 20140909 J-G: this is a class that is in need of tests in general
				// TODO 20140909 J-G: i have some questions around the BlockChainValidator / BlockChainScorer / BlockExecutor / Foraging interplay that might best be answered in IRC

				// possibilities, unlocked account is:
				//  real, but has eligible remote = reject
				//  real, but no eligible remote = harvest with real
				//  virtual, but is not eligible = reject
				//  virtual and is eligible = harvest with virtual
				for (final Account virtualForger : this.unlockedAccounts) {
					final BlockHeight forgedBlockHeight = lastBlock.getHeight().next();
					final PoiAccountState accountState = this.poiFacade.findStateByAddress(virtualForger.getAddress());

					Account forgerOwner = virtualForger;
					final RemoteLinks remoteLinks = accountState.getRemoteLinks();
					// TODO BUG TODO
					//if (!BlockChainValidator.canAccountForageAtHeight(forgerState, forgedBlockHeight)) {
					//	continue;
					//}

					if (remoteLinks.isRemoteHarvester()) {
						forgerOwner = this.accountLookup.findByAddress(remoteLinks.getCurrent().getLinkedAddress());
					}

					// Don't allow a harvester to include his own transactions
					final Collection<Transaction> eligibleTxList = this.filterTransactionsForHarvester(transactionList, forgerOwner);

					// unlocked accounts are only dummies, so we need to find REAL accounts to get the balance
					final Block newBlock = this.createSignedBlock(blockTime, eligibleTxList, lastBlock, virtualForger, difficulty);

					LOGGER.info(String.format("generated signature: %s", newBlock.getSignature()));

					final BigInteger hit = blockScorer.calculateHit(newBlock);
					LOGGER.info("   hit: 0x" + hit.toString(16));
					final BigInteger target = blockScorer.calculateTarget(lastBlock, newBlock);
					LOGGER.info("target: 0x" + target.toString(16));
					LOGGER.info("difficulty: " + (difficulty.getRaw() * 100L) / BlockDifficulty.INITIAL_DIFFICULTY.getRaw() + "%");

					if (hit.compareTo(target) < 0) {
						LOGGER.info("[HIT] forger balance: " + blockScorer.calculateForgerBalance(newBlock));
						LOGGER.info("[HIT] last block: " + dbLastBlock.getShortId());
						LOGGER.info("[HIT] timestamp diff: " + newBlock.getTimeStamp().subtract(lastBlock.getTimeStamp()));
						LOGGER.info("[HIT] block diff: " + newBlock.getDifficulty());

						final long score = blockScorer.calculateBlockScore(lastBlock, newBlock);
						if (score > bestScore) {
							bestBlock = newBlock;
							bestScore = score;
						}
					}
				}
			} // synchronized
		} catch (final RuntimeException e) {
			LOGGER.warning("exception occurred during generation of a block");
			LOGGER.warning(e.toString());
		}

		return bestBlock;
	}

	private BlockDifficulty calculateDifficulty(final BlockScorer scorer, final Block lastBlock) {
		final BlockHeight blockHeight = new BlockHeight(Math.max(1L, lastBlock.getHeight().getRaw() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION + 1));
		final int limit = (int)Math.min(lastBlock.getHeight().getRaw(), (BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION));
		final List<TimeInstant> timeStamps = this.blockDao.getTimeStampsFrom(blockHeight, limit);
		final List<BlockDifficulty> difficulties = this.blockDao.getDifficultiesFrom(blockHeight, limit);

		return scorer.getDifficultyScorer().calculateDifficulty(difficulties, timeStamps, lastBlock.getHeight().getRaw() + 1);
	}

	public Block createSignedBlock(
			final TimeInstant blockTime,
			final Collection<Transaction> transactionList,
			final Block lastBlock,
			final Account virtualForger,
			final BlockDifficulty difficulty) {
		final Account forger = this.accountLookup.findByAddress(virtualForger.getAddress());

		// TODO: Probably better to include difficulty in the block constructor?
		final Block newBlock = new Block(forger, lastBlock, blockTime);

		newBlock.setDifficulty(difficulty);
		if (!transactionList.isEmpty()) {
			newBlock.addTransactions(transactionList);
		}

		newBlock.signBy(virtualForger);
		return newBlock;
	}
}
