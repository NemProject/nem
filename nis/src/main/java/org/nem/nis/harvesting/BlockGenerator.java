package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.poi.*;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service for generating a new block.
 */
public class BlockGenerator {
	private static final Logger LOGGER = Logger.getLogger(BlockGenerator.class.getName());
	private final AccountLookup accountLookup;
	private final PoiFacade poiFacade;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final BlockDao blockDao;

	/**
	 * Creates a new block generator.
	 *
	 * @param accountLookup The account lookup.
	 * @param poiFacade The poi facade.
	 * @param unconfirmedTransactions The unconfirmed transactions.
	 * @param blockDao The block dao.
	 */
	public BlockGenerator(
			final AccountLookup accountLookup,
			final PoiFacade poiFacade,
			final UnconfirmedTransactions unconfirmedTransactions,
			final BlockDao blockDao) {
		this.accountLookup = accountLookup;
		this.poiFacade = poiFacade;
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.blockDao = blockDao;
	}

	/**
	 * Generates the next block.
	 *
	 * @param lastBlock The last block.
	 * @param harvesterAddress The harvester address.
	 * @param blockScorer The block scorer.
	 * @param blockTime The block time.
	 * @return The block.
	 */
	public GeneratedBlock generateNextBlock(
			final Block lastBlock,
			final Address harvesterAddress,
			final BlockScorer blockScorer,
			final TimeInstant blockTime) {
		final Block newBlock = this.createBlock(lastBlock, harvesterAddress, blockScorer, blockTime);
		LOGGER.info(String.format("generated signature: %s", newBlock.getSignature()));

		final BigInteger hit = blockScorer.calculateHit(newBlock);
		LOGGER.info("   hit: 0x" + hit.toString(16));
		final BigInteger target = blockScorer.calculateTarget(lastBlock, newBlock);
		LOGGER.info("target: 0x" + target.toString(16));
		LOGGER.info("difficulty: " + (newBlock.getDifficulty().getRaw() * 100L) / BlockDifficulty.INITIAL_DIFFICULTY.getRaw() + "%");

		if (hit.compareTo(target) >= 0) {
			return null;
		}

		LOGGER.info(String.format("[HIT] forger balance: %s", blockScorer.calculateForgerBalance(newBlock)));
		LOGGER.info(String.format("[HIT] last block: %s", newBlock.getPreviousBlockHash()));
		LOGGER.info(String.format("[HIT] timestamp diff: %s", newBlock.getTimeStamp().subtract(lastBlock.getTimeStamp())));
		LOGGER.info(String.format("[HIT] block diff: %s", newBlock.getDifficulty()));
		final long score = blockScorer.calculateBlockScore(lastBlock, newBlock);
		return new GeneratedBlock(newBlock, score);
	}

	private Block createBlock(
			final Block lastBlock,
			final Address harvesterAddress,
			final BlockScorer blockScorer,
			final TimeInstant blockTime) {
		final BlockHeight harvestedBlockHeight = lastBlock.getHeight().next();
		final PoiAccountState accountState = this.poiFacade.findForwardedStateByAddress(harvesterAddress, harvestedBlockHeight);
		final Account harvester = this.accountLookup.findByAddress(accountState.getAddress());
		final Collection<Transaction> transactions = this.unconfirmedTransactions.getTransactionsForNewBlock(harvesterAddress, blockTime).getAll();
		final BlockDifficulty difficulty = this.calculateDifficulty(blockScorer, lastBlock.getHeight());

		final Block newBlock = new Block(harvester, lastBlock, blockTime);
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
}
