package org.nem.nis;

import com.sun.java_cup.internal.runtime.lr_parser;
import org.nem.core.crypto.Hashes;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ArrayUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Provides functions for scoring block hits and targets.
 */
public class BlockScorer {
	private static final Logger LOGGER = Logger.getLogger(BlockScorer.class.getName());

	/**
	 * The target time between two blocks in seconds.
	 */
	private static final long TARGET_TIME_BETWEEN_BLOCKS = 86400L / BlockChain.ESTIMATED_BLOCKS_PER_DAY;

	/**
	 * BigInteger constant 2^64
	 */
    public static final BigInteger TWO_TO_THE_POWER_OF_64 = new BigInteger("18446744073709551616");

	/**
	 * Number of blocks which the calculation of difficulty should include 
	 */
    public static final long NUM_BLOCKS_FOR_AVERAGE_CALCULATION = 60;

    /**
	 * Calculates the hit score for block.
	 *
	 * @param block The block.
	 * @return the hit score.
	 */
	public BigInteger calculateHit(final Block block) {
		return new BigInteger(1, Arrays.copyOfRange(block.getGenerationHash().getRaw(), 10, 18));
	}

	/**
	 * Calculates the target score for block given the previous block using an external block signer account.
	 *
	 * @param prevBlock The previous block.
	 * @param block The block.
	 * @return The target score.
	 */
	public BigInteger calculateTarget(final Block prevBlock, final Block block) {
		int timeStampDifference = block.getTimeStamp().subtract(prevBlock.getTimeStamp());
		if (timeStampDifference < 0)
			return BigInteger.ZERO;

		long forgerBalance = block.getSigner().getBalance().getNumNem();
		return BigInteger.valueOf(timeStampDifference)
						 .multiply(BigInteger.valueOf(forgerBalance))
						 .multiply(TWO_TO_THE_POWER_OF_64)
						 .divide(block.getDifficulty().asBigInteger());
	}

	/**
	 * Calculates the block score for the specified block.
	 *
	 * @param currentBlock The currently analyzed block.
	 *
	 * @return The block score.
	 */
//	public long calculateBlockScore(final Block currentBlock) {
//		final Account account = currentBlock.getSigner();
//		final long foragedBlocks = account.getForagedBlocks().getRaw();
//		return calculateBlockScoreImpl(1, foragedBlocks, currentBlock.getDifficulty().getRaw());
//	}

	public long calculateBlockScore(final Block parentBlock, final Block currentBlock) {
		final int timeDiff = currentBlock.getTimeStamp().subtract(parentBlock.getTimeStamp());
		final Account account = currentBlock.getSigner();
		final long foragedBlocks = account.getForagedBlocks().getRaw();
		return calculateBlockScoreImpl(timeDiff, foragedBlocks, currentBlock.getDifficulty().getRaw());
	}

	private long calculateBlockScoreImpl(int timeDiff, long foragedBlocks, long difficulty) {
		return difficulty / (timeDiff + 4 * foragedBlocks);
	}

	/**
	 * Calculates the difficulty based the last n blocks.
	 * 
	 * @param difficulties historical difficulties.
	 * @param timestamps historical timestamps.
	 *
	 * @return The difficulty for the next block.
	 */
	public BlockDifficulty calculateDifficulty(final List<BlockDifficulty> difficulties, final List<TimeInstant> timestamps) {
		if (difficulties.size() < 2) {
			return BlockDifficulty.INITIAL_DIFFICULTY;
		}

		final TimeInstant newestTimestamp = timestamps.get(timestamps.size() - 1);
		final TimeInstant oldestTimestamp = timestamps.get(0);
		final long timeDiff = newestTimestamp.subtract(oldestTimestamp);
		final long heightDiff = difficulties.size();
		long averageDifficulty = 0;
		for (final BlockDifficulty diff : difficulties) {
			averageDifficulty += diff.getRaw();
		}

		averageDifficulty /= heightDiff;

		long difficulty = BigInteger.valueOf(averageDifficulty).multiply(BigInteger.valueOf(TARGET_TIME_BETWEEN_BLOCKS))
															   .multiply(BigInteger.valueOf(heightDiff))
															   .divide(BigInteger.valueOf(timeDiff))
															   .longValue();

		return new BlockDifficulty(difficulty);
	}
}
