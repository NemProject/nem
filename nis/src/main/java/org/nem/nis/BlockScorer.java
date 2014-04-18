package org.nem.nis;

import org.nem.core.crypto.Hashes;
import org.nem.core.model.*;
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
	 * 1_000_000_000 NEMs have force to generate block every minute.
	 */
	public static final long INITIAL_DIFFICULTY = 120000000000L;
	
	/**
	 * Minimum value for difficulty.
	 */
	private static final long MIN_DIFFICULTY = INITIAL_DIFFICULTY / 10L;
	
	/**
	 * Maximum value for difficulty.
	 */
	private static final long MAX_DIFFICULTY = INITIAL_DIFFICULTY * 10L;
	
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
						 .divide(BigInteger.valueOf(block.getDifficulty()));
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
		return calculateBlockScoreImpl(timeDiff, currentBlock.getDifficulty());
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
	 * @param historicalBlocks historical blocks, i.e. the last n blocks.
	 *
	 * @return The difficulty for the next block.
	 */
	public long calculateDifficulty(final List<Block> historicalBlocks) {
		if (historicalBlocks.size() < 2) {
			return INITIAL_DIFFICULTY;
		}
		else {
			Block lastBlock = historicalBlocks.get(historicalBlocks.size() - 1);
			Block firstBlock = historicalBlocks.get(0);
			long timeDiff = lastBlock.getTimeStamp().subtract(firstBlock.getTimeStamp());
			final long heightDiff = historicalBlocks.size();
			long averageDifficulty = 0;
			for (Block block : historicalBlocks) {
				averageDifficulty += block.getDifficulty();
			}
			averageDifficulty /= heightDiff;
			
			long difficulty = BigInteger.valueOf(averageDifficulty).multiply(BigInteger.valueOf(TARGET_TIME_BETWEEN_BLOCKS))
																   .multiply(BigInteger.valueOf(heightDiff))
																   .divide(BigInteger.valueOf(timeDiff))
																   .longValue();
            if (difficulty < MIN_DIFFICULTY) {
            	difficulty = MIN_DIFFICULTY;
            }
            if (difficulty > MAX_DIFFICULTY) {
            	difficulty = MAX_DIFFICULTY;
            }
            return difficulty;
		}
	}
}
