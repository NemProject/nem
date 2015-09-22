package org.nem.nis.harvesting;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockScorer;
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
	private final NewBlockTransactionsProvider transactionsProvider;
	private final BlockDao blockDao;
	private final BlockScorer blockScorer;
	private final BlockValidator blockValidator;

	/**
	 * Creates a new block generator.
	 *
	 * @param nisCache The NIS cache.
	 * @param transactionsProvider The new block transactions provider.
	 * @param blockDao The block dao.
	 * @param blockScorer The block scorer.
	 * @param blockValidator The block validator.
	 */
	public BlockGenerator(
			final ReadOnlyNisCache nisCache,
			final NewBlockTransactionsProvider transactionsProvider,
			final BlockDao blockDao,
			final BlockScorer blockScorer,
			final BlockValidator blockValidator) {
		this.nisCache = nisCache;
		this.transactionsProvider = transactionsProvider;
		this.blockDao = blockDao;
		this.blockScorer = blockScorer;
		this.blockValidator = blockValidator;
	}

	/**
	 * Generates the next block.
	 *
	 * @param lastBlock The last block.
	 * @param harvesterAccount The harvester account.
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

		LOGGER.info(String.format("[HIT] harvester effective importance: %s", this.blockScorer.calculateHarvesterEffectiveImportance(newBlock)));
		LOGGER.info(String.format("[HIT] last block: %s", newBlock.getPreviousBlockHash()));
		LOGGER.info(String.format("[HIT] timestamp diff: %s", newBlock.getTimeStamp().subtract(lastBlock.getTimeStamp())));
		LOGGER.info(String.format("[HIT] block diff: %s", newBlock.getDifficulty()));
		final long score = this.blockScorer.calculateBlockScore(lastBlock, newBlock);
		return new GeneratedBlock(newBlock, score);
	}

	/**
	 * Gets a value indicating whether or not the harvester is allowed to generate the next block.
	 *
	 * @param lastBlock The last block.
	 * @param harvesterAccount The harvester account.
	 * @param blockTime The block time.
	 * @return The block.
	 */
	public boolean isAllowedToGenerateNewBlock(
			final Block lastBlock,
			final Account harvesterAccount,
			final TimeInstant blockTime) {
		final BlockHeight harvestedBlockHeight = lastBlock.getHeight().next();
		final Hash generationHash = HashUtils.nextHash(lastBlock.getGenerationHash(), harvesterAccount.getAddress().getPublicKey());
		final BigInteger hit = this.blockScorer.calculateHit(generationHash);
		final BigInteger target = this.blockScorer.calculateTarget(
				lastBlock,
				harvesterAccount,
				harvestedBlockHeight,
				blockTime,
				this. calculateDifficulty(this.blockScorer, lastBlock.getHeight()));
		return hit.compareTo(target) < 0;
	}

	private Block createBlock(
			final Block lastBlock,
			final Account harvesterAccount,
			final BlockScorer blockScorer,
			final TimeInstant blockTime) {
		final BlockHeight harvestedBlockHeight = lastBlock.getHeight().next();
		final Account ownerAccount = this.getOwnerAccount(harvesterAccount, harvestedBlockHeight);

		final Collection<Transaction> transactions = this.transactionsProvider.getBlockTransactions(
				ownerAccount.getAddress().equals(harvesterAccount.getAddress()) ? harvesterAccount.getAddress() : ownerAccount.getAddress(),
				blockTime,
				harvestedBlockHeight);
		final BlockDifficulty difficulty = this.calculateDifficulty(blockScorer, lastBlock.getHeight());

		final Block newBlock = new Block(harvesterAccount, lastBlock, blockTime);
		newBlock.setLessor(ownerAccount);
		newBlock.setDifficulty(difficulty);
		newBlock.addTransactions(transactions);
		newBlock.sign();
		return newBlock;
	}

	private Account getOwnerAccount(final Account account, final BlockHeight height) {
		final ReadOnlyAccountState ownerState = this.nisCache.getAccountStateCache().findForwardedStateByAddress(
				account.getAddress(),
				height);
		return this.nisCache.getAccountCache().findByAddress(ownerState.getAddress());
	}

	private BlockDifficulty calculateDifficulty(final BlockScorer scorer, final BlockHeight lastBlockHeight) {
		final BlockHeight blockHeight = new BlockHeight(Math.max(1L, lastBlockHeight.getRaw() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION + 1));
		final int limit = (int)Math.min(lastBlockHeight.getRaw(), BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION);
		final List<TimeInstant> timeStamps = this.blockDao.getTimeStampsFrom(blockHeight, limit);
		final List<BlockDifficulty> difficulties = this.blockDao.getDifficultiesFrom(blockHeight, limit);
		return scorer.getDifficultyScorer().calculateDifficulty(difficulties, timeStamps, lastBlockHeight.getRaw() + 1);
	}
}
