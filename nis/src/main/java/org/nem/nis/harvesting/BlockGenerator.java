package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.validators.BlockValidator;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service for generating a new block.
 */
public class BlockGenerator {
	private static final Logger LOGGER = Logger.getLogger(BlockGenerator.class.getName());
	private final ReadOnlyNisCache nisCache;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final BlockDao blockDao;
	private final BlockScorer blockScorer;
	private final BlockValidator blockValidator;

	/**
	 * Creates a new block generator.
	 *
	 * @param nisCache The NIS cache.
	 * @param unconfirmedTransactions The unconfirmed transactions.
	 * @param blockDao The block dao.
	 */
	public BlockGenerator(
			final ReadOnlyNisCache nisCache,
			final UnconfirmedTransactions unconfirmedTransactions,
			final BlockDao blockDao,
			final BlockScorer blockScorer,
			final BlockValidator blockValidator) {
		this.nisCache = nisCache;
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.blockDao = blockDao;
		this.blockScorer = blockScorer;
		this.blockValidator = blockValidator;
	}

	/**
	 * Generates the next block.
	 *
	 * @param lastBlock The last block.
	 * @param harvesterAccount The harvester address.
	 * @param blockTime The block time.
	 * @return The block.
	 */
	public GeneratedBlock generateNextBlock(
			final Block lastBlock,
			final Account harvesterAccount,
			final TimeInstant blockTime) {
		final Block newBlock = this.createBlock(lastBlock, harvesterAccount, this.blockScorer, blockTime);
		LOGGER.info(String.format("generated signature: %s", newBlock.getSignature()));

		final BigInteger hit = this.blockScorer.calculateHit(newBlock);
		LOGGER.info("   hit: 0x" + hit.toString(16));
		final BigInteger target = this.blockScorer.calculateTarget(lastBlock, newBlock);
		LOGGER.info("target: 0x" + target.toString(16));
		LOGGER.info("difficulty: " + (newBlock.getDifficulty().getRaw() * 100L) / BlockDifficulty.INITIAL_DIFFICULTY.getRaw() + "%");

		if (hit.compareTo(target) >= 0) {
			return null;
		}

		final ValidationResult result = this.blockValidator.validate(newBlock);
		if (!result.isSuccess()) {
			LOGGER.severe(String.format("generated block did not pass validation: %s", result));
			return null;
		}

		LOGGER.info(String.format("[HIT] harvester balance: %s", this.blockScorer.calculateHarvesterBalance(newBlock)));
		LOGGER.info(String.format("[HIT] last block: %s", newBlock.getPreviousBlockHash()));
		LOGGER.info(String.format("[HIT] timestamp diff: %s", newBlock.getTimeStamp().subtract(lastBlock.getTimeStamp())));
		LOGGER.info(String.format("[HIT] block diff: %s", newBlock.getDifficulty()));
		final long score = this.blockScorer.calculateBlockScore(lastBlock, newBlock);
		return new GeneratedBlock(newBlock, score);
	}

	private Block createBlock(
			final Block lastBlock,
			final Account harvesterAccount,
			final BlockScorer blockScorer,
			final TimeInstant blockTime) {
		final BlockHeight harvestedBlockHeight = lastBlock.getHeight().next();
		final ReadOnlyAccountState ownerState = this.nisCache.getAccountStateCache().findForwardedStateByAddress(
				harvesterAccount.getAddress(),
				harvestedBlockHeight);
		final Account ownerAccount = this.nisCache.getAccountCache().findByAddress(ownerState.getAddress());

		final Collection<Transaction> transactions = this.unconfirmedTransactions
				.getTransactionsForNewBlock(ownerAccount.getAddress(), blockTime)
				.getMostImportantTransactions(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK(harvestedBlockHeight));
		final BlockDifficulty difficulty = this.calculateDifficulty(blockScorer, lastBlock.getHeight());

		// it's the remote harvester that generates a block NOT owner, we won't have owner's key here!
		// TODO 20150109 G-*: minor micro-optimization:
		// > this causes the call Block.setPrevious, which in turn calculates hash of lastBlock
		// > (which causes serialization of the lastBlock every time)
		// > I think we could avoid that
		final Block newBlock = new Block(harvesterAccount, lastBlock, blockTime);
		newBlock.setLessor(ownerAccount);
		newBlock.setDifficulty(difficulty);
		newBlock.addTransactions(transactions);
		newBlock.sign();
		return newBlock;
	}

	private BlockDifficulty calculateDifficulty(final BlockScorer scorer, final BlockHeight lastBlockHeight) {
		final BlockHeight blockHeight = new BlockHeight(Math.max(1L, lastBlockHeight.getRaw() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION + 1));
		final int limit = (int)Math.min(lastBlockHeight.getRaw(), BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION);
		final List<TimeInstant> timeStamps = this.blockDao.getTimeStampsFrom(blockHeight, limit);
		final List<BlockDifficulty> difficulties = this.blockDao.getDifficultiesFrom(blockHeight, limit);
		return scorer.getDifficultyScorer().calculateDifficulty(difficulties, timeStamps, lastBlockHeight.getRaw() + 1);
	}

	/**
	 * Drops transactions that have already expired.
	 *
	 * @param time The current time.
	 */
	public void dropExpireTransactions(final TimeInstant time) {
		this.unconfirmedTransactions.dropExpiredTransactions(time);
	}
}
