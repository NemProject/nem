package org.nem.nis;

import org.nem.core.crypto.Hashes;
import org.nem.core.model.*;
import org.nem.core.utils.ArrayUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * Provides functions for scoring block hits and targets.
 */
public class BlockScorer {
	
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
	 * @param prevBlock The block.
	 * @return the hit score.
	 */
	public BigInteger calculateHit(final Block prevBlock, final Account blockSigner) {
		byte[] hash = Hashes.sha3(ArrayUtils.concat(blockSigner.getKeyPair().getPublicKey().getRaw(), HashUtils.calculateHash(prevBlock).getRaw()));
		return new BigInteger(1, Arrays.copyOfRange(hash, 10, 18));
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
						 .divide(BigInteger.valueOf(block.getDifficulty().getRaw()));
	}

	/**
	 * Calculates the block score for block.
	 *
	 * @param parentBlock previous block in the chain.
	 * @param currentBlock currently analyzed block.
	 *
	 * @return The block score.
	 */
	public long calculateBlockScore(final Block parentBlock, final Block currentBlock) {
		int timeDiff = currentBlock.getTimeStamp().subtract(parentBlock.getTimeStamp());
		return calculateBlockScoreImpl(timeDiff, currentBlock.getDifficulty().getRaw());
	}

	public long calculateBlockScore(final org.nem.nis.dbmodel.Block parentBlock, final org.nem.nis.dbmodel.Block currentBlock) {
		int timeDiff = (currentBlock.getTimestamp() - parentBlock.getTimestamp());
		return calculateBlockScoreImpl(timeDiff, currentBlock.getDifficulty());
	}

	/**
	 * @param timeDiff positive time difference between blocks
	 */
	private long calculateBlockScoreImpl(int timeDiff, long difficulty) {
		return difficulty;
	}

	/**
	 * Calculates the difficulty based the last n blocks.
	 * 
	 * @param difficulties historical difficulties.
	 * @param timestamps historical timestamps.
	 *
	 * @return The difficulty for the next block.
	 */
	public BlockDifficulty calculateDifficulty(final List<Long> difficulties, final List<Integer> timestamps) {
		if (difficulties.size() < 2) {
			return BlockDifficulty.INITIAL_DIFFICULTY;
		}

		Integer newestTimestamp = timestamps.get(timestamps.size() - 1);
		Integer oldestTimestamp = timestamps.get(0);
		long timeDiff = newestTimestamp.longValue() - oldestTimestamp.longValue();
		final long heightDiff = difficulties.size();
		long averageDifficulty = 0;
		for (Long diff : difficulties) {
			averageDifficulty += diff;
		}
		averageDifficulty /= heightDiff;

		long difficulty = BigInteger.valueOf(averageDifficulty).multiply(BigInteger.valueOf(TARGET_TIME_BETWEEN_BLOCKS))
															   .multiply(BigInteger.valueOf(heightDiff))
															   .divide(BigInteger.valueOf(timeDiff))
															   .longValue();

		return new BlockDifficulty(difficulty);
	}
}
