package org.nem.nis.harvesting;

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
	public BlockGenerator(final ReadOnlyNisCache nisCache, final NewBlockTransactionsProvider transactionsProvider, final BlockDao blockDao,
			final BlockScorer blockScorer, final BlockValidator blockValidator) {
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
	public GeneratedBlock generateNextBlock(final Block lastBlock, final Account harvesterAccount, final TimeInstant blockTime) {
		final GenerationState state = new GenerationState(this, lastBlock, harvesterAccount, blockTime);
		if (!state.isHit()) {
			return null;
		}

		final Block newBlock = state.createBlock();
		final ValidationResult result = this.blockValidator.validate(newBlock);
		if (!result.isSuccess()) {
			LOGGER.severe(String.format("generated block did not pass validation: %s", result));
			return null;
		}

		LOGGER.info(String.format("[HIT] harvester: %s", harvesterAccount.getAddress()));
		LOGGER.info(String.format("[HIT] harvester effective importance: %s",
				this.blockScorer.calculateHarvesterEffectiveImportance(newBlock)));
		LOGGER.info(String.format("[HIT] last block: %s", newBlock.getPreviousBlockHash()));
		LOGGER.info(String.format("[HIT] timestamp diff: %s", newBlock.getTimeStamp().subtract(lastBlock.getTimeStamp())));
		LOGGER.info(String.format("[HIT] block diff: %s", newBlock.getDifficulty()));
		final long score = this.blockScorer.calculateBlockScore(lastBlock, newBlock);
		return new GeneratedBlock(newBlock, score);
	}

	private static class GenerationState {
		private final ReadOnlyNisCache nisCache;
		private final NewBlockTransactionsProvider transactionsProvider;
		private final BlockDao blockDao;
		private final BlockScorer blockScorer;

		private final Block lastBlock;
		private final Account harvesterAccount;
		private final TimeInstant blockTime;

		private final BlockHeight harvestedBlockHeight;
		private final Account ownerAccount;
		private final Block newBlock;

		public GenerationState(final BlockGenerator generator, final Block lastBlock, final Account harvesterAccount,
				final TimeInstant blockTime) {
			this.nisCache = generator.nisCache;
			this.transactionsProvider = generator.transactionsProvider;
			this.blockDao = generator.blockDao;
			this.blockScorer = generator.blockScorer;

			this.lastBlock = lastBlock;
			this.harvesterAccount = harvesterAccount;
			this.blockTime = blockTime;

			this.harvestedBlockHeight = this.lastBlock.getHeight().next();
			this.ownerAccount = this.getOwnerAccount(this.harvesterAccount, this.harvestedBlockHeight);
			final BlockDifficulty difficulty = this.calculateDifficulty(this.blockScorer, this.lastBlock.getHeight());

			this.newBlock = new Block(this.harvesterAccount, this.lastBlock, this.blockTime);
			this.newBlock.setLessor(this.ownerAccount);
			this.newBlock.setDifficulty(difficulty);
		}

		public boolean isHit() {
			final BigInteger hit = this.blockScorer.calculateHit(this.newBlock);
			final BigInteger target = this.blockScorer.calculateTarget(this.lastBlock, this.newBlock);
			return hit.compareTo(target) < 0;
		}

		public Block createBlock() {
			final Address harvesterAddress = this.ownerAccount.getAddress().equals(this.harvesterAccount.getAddress())
					? this.harvesterAccount.getAddress()
					: this.ownerAccount.getAddress();
			final Collection<Transaction> transactions = this.transactionsProvider.getBlockTransactions(harvesterAddress, this.blockTime,
					this.harvestedBlockHeight);
			this.newBlock.addTransactions(transactions);
			this.newBlock.sign();
			return this.newBlock;
		}

		private Account getOwnerAccount(final Account account, final BlockHeight height) {
			final ReadOnlyAccountState ownerState = this.nisCache.getAccountStateCache().findForwardedStateByAddress(account.getAddress(),
					height);
			return this.nisCache.getAccountCache().findByAddress(ownerState.getAddress());
		}

		private BlockDifficulty calculateDifficulty(final BlockScorer scorer, final BlockHeight lastBlockHeight) {
			final BlockHeight blockHeight = new BlockHeight(
					Math.max(1L, lastBlockHeight.getRaw() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION + 1));
			final int limit = (int) Math.min(lastBlockHeight.getRaw(), BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION);
			final List<TimeInstant> timeStamps = this.blockDao.getTimeStampsFrom(blockHeight, limit);
			final List<BlockDifficulty> difficulties = this.blockDao.getDifficultiesFrom(blockHeight, limit);
			return scorer.getDifficultyScorer().calculateDifficulty(difficulties, timeStamps);
		}
	}
}
