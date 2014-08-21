package org.nem.nis;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dao.*;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.poi.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
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

	private final ConcurrentHashSet<Account> unlockedAccounts;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final AccountLookup accountLookup;
	private final PoiFacade poiFacade;
	private final BlockDao blockDao;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final TransferDao transferDao;

	@Autowired(required = true)
	public Foraging(
			final AccountLookup accountLookup,
			final PoiFacade poiFacade,
			final BlockDao blockDao,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final TransferDao transferDao) {
		this.accountLookup = accountLookup;
		this.poiFacade = poiFacade;
		this.blockDao = blockDao;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.transferDao = transferDao;

		this.unlockedAccounts = new ConcurrentHashSet<>();
		this.unconfirmedTransactions = new UnconfirmedTransactions();
	}

	/**
	 * Unlocks the specified account for foraging.
	 *
	 * @param account The account.
	 */
	public UnlockResult addUnlockedAccount(final Account account) {
		if (!this.accountLookup.isKnownAddress(account.getAddress())) {
			return UnlockResult.FAILURE_UNKNOWN_ACCOUNT;
		}

		final PoiAccountInfo accountInfo = new PoiAccountInfo(
				-1,
				this.poiFacade.findStateByAddress(account.getAddress()),
				new BlockHeight(this.blockChainLastBlockLayer.getLastBlockHeight()));
		if (!accountInfo.canForage()) {
			return UnlockResult.FAILURE_FORAGING_INELIGIBLE;
		}

		this.unlockedAccounts.add(account);
		return UnlockResult.SUCCESS;
	}

	/**
	 * Removes the specified account from the list of active foraging accounts.
	 *
	 * @param account The account.
	 */
	public void removeUnlockedAccount(final Account account) {
		if (this.accountLookup.isKnownAddress(account.getAddress())) {
			this.unlockedAccounts.remove(account);
		}
	}

	/**
	 * Determines if a given account is unlocked.
	 *
	 * @param account The account.
	 * @return true if the account is unlocked, false otherwise.
	 */
	public boolean isAccountUnlocked(final Account account) {
		return this.unlockedAccounts.contains(account);
	}

	/**
	 * Determines if a given account is unlocked.
	 *
	 * @param address The account address.
	 * @return true if the account is unlocked, false otherwise.
	 */
	public boolean isAccountUnlocked(final Address address) {
		for (final Account account : this.unlockedAccounts) {
			if (account.getAddress().equals(address)) {
				return true;
			}
		}

		return false;
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
		return this.unconfirmedTransactions.add(transaction);
	}

	private ValidationResult addUnconfirmedTransaction(final Transaction transaction) {
		return this.unconfirmedTransactions.add(transaction, hash -> {
			synchronized (this.blockChainLastBlockLayer) {
				return null != this.transferDao.findByHash(hash.getRaw());
			}
		});
	}

	/**
	 * Removes all block's transactions from list of unconfirmed transactions
	 *
	 * @param block
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
		// TODO-CR: J->BR (reformatting) it's kind of unreadable having everything on a single line
		// 08072014: BR -> J I agree, but it seems eclipse does not enforce this rule.
		//                   Even worse, even if I format manually it reverts my changes :/
		//                   I need to turn the formatter off here.
		// TODO-CR: J->BR were you able to get intellij formatting working?
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

	public List<Transaction> getUnconfirmedTransactionsForNewBlock(final TimeInstant blockTime) {
		return this.unconfirmedTransactions.removeConflictingTransactions(
				this.unconfirmedTransactions.getTransactionsBefore(blockTime));
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
	 * returns foraged block or null
	 *
	 * @return
	 */
	public Block forageBlock(final BlockScorer blockScorer) {
		if (this.blockChainLastBlockLayer.getLastDbBlock() == null) {
			return null;
		}

		LOGGER.fine("block generation " + Integer.toString(this.unconfirmedTransactions.size()) + " " + Integer.toString(this.unlockedAccounts.size()));

		Block bestBlock = null;
		long bestScore = Long.MIN_VALUE;
		// because of access to unconfirmedTransactions, and lastBlock*

		final TimeInstant blockTime = NisMain.TIME_PROVIDER.getCurrentTime();
		this.unconfirmedTransactions.dropExpiredTransactions(blockTime);
		final Collection<Transaction> transactionList = this.getUnconfirmedTransactionsForNewBlock(blockTime);
		try {
			synchronized (this.blockChainLastBlockLayer) {
				final org.nem.nis.dbmodel.Block dbLastBlock = this.blockChainLastBlockLayer.getLastDbBlock();
				final Block lastBlock = BlockMapper.toModel(dbLastBlock, this.accountLookup);
				final BlockDifficulty difficulty = this.calculateDifficulty(blockScorer, lastBlock);

				for (final Account virtualForger : this.unlockedAccounts) {
					// Don't allow a harvester to include his own transactions
					final Collection<Transaction> eligibleTxList = this.filterTransactionsForHarvester(transactionList, virtualForger);

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

		if (lastBlock.getHeight().getRaw() + 1 >= BlockMarkerConstants.DIFFICULTY_FIX_HEIGHT) {
			return scorer.getDifficultyScorer().calculateDifficultyNew(difficulties, timeStamps);
		} else {
			return scorer.getDifficultyScorer().calculateDifficulty(difficulties, timeStamps);
		}
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
