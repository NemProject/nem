package org.nem.nis;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;

import java.math.BigInteger;
import java.util.List;

/**
 * Provides functions for scoring block hits and targets.
 */
public class BlockScorer {

	/**
	 * The target time between two blocks in seconds.
	 */
	private static final long TARGET_TIME_BETWEEN_BLOCKS = 86400L / BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;

	/**
	 * BigInteger constant 2^64
	 */
    public static final BigInteger TWO_TO_THE_POWER_OF_64 = new BigInteger("18446744073709551616");

	/**
	 * BigInteger constant 2^56
	 */
    public static final long TWO_TO_THE_POWER_OF_54 = 18014398509481984L;

	/**
	 * Number of blocks which the calculation of difficulty should include 
	 */
    public static final int NUM_BLOCKS_FOR_AVERAGE_CALCULATION = 60;

    /**
     * Helper constant calculating the logarithm of BigInteger
     */
    private static final double TWO_TO_THE_POWER_OF_256 = Math.pow(2.0, 256.0);

	/**
	 * Number of blocks that should be treated as a group for POI purposes.
	 * In other words, POI importances will only be calculated at blocks that
	 * are a multiple of this grouping number.
	 */
	private static final int POI_GROUPING = 31;

	public final AccountAnalyzer accountAnalyzer;

	public BlockScorer(final AccountAnalyzer accountAnalyzer) {
		this.accountAnalyzer = accountAnalyzer;
	}

	/**
	 * Forces a recalculation of the importances the next time calculateForgerBalance() is called.
	 */
	public void forceImportanceCalculation() {
		this.accountAnalyzer.resetLastPoiRecalc();
	}

	/**
	 * Calculates the hit score for block.
	 *
	 * @param block The block.
	 * @return the hit score.
	 */
	public BigInteger calculateHit(final Block block) {
		BigInteger val = new BigInteger(1, block.getGenerationHash().getRaw());
		double tmp = Math.abs(Math.log(val.doubleValue()/TWO_TO_THE_POWER_OF_256));
		val = BigInteger.valueOf((long)(TWO_TO_THE_POWER_OF_54 * tmp));
		return val;
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

		long forgerBalance = calculateForgerBalance(block);
		return BigInteger.valueOf(timeStampDifference)
				 .multiply(BigInteger.valueOf(forgerBalance))
				 .multiply(TWO_TO_THE_POWER_OF_64)
				 .divide(block.getDifficulty().asBigInteger());
	}

	/**
	 * Gets the grouped height for the specified un-grouped height.
	 *
	 * @param height The un-grouped height.
	 * @return The grouped height.
	 */
	public BlockHeight getGroupedHeight(final BlockHeight height) {
		final long backInTime = height.getRaw() - 1;
		final long grouped = (backInTime / POI_GROUPING) * POI_GROUPING;
		return 0 == grouped ? BlockHeight.ONE : new BlockHeight(grouped);
	}

	/**
	 * Calculates forager balance for block.
	 * This has the side-effect of recalculating importances.
	 *
	 * @param block The signed, "hit" block.
	 * @return The forager balance.
	 */
	public long calculateForgerBalance(final Block block) {
		final BlockHeight blockHeight = this.getGroupedHeight(block.getHeight());
		this.accountAnalyzer.recalculateImportances(blockHeight);
		final long multiplier = NemesisBlock.AMOUNT.getNumNem();
		return (long)(block.getSigner().getImportanceInfo().getImportance(blockHeight) * multiplier);
	}

	/**
	 * Calculates the block score for the specified block.
	 *
	 * @param currentBlock The currently analyzed block.
	 *
	 * @return The block score.
	 */
	public long calculateBlockScore(final Block parentBlock, final Block currentBlock) {
		final int timeDiff = currentBlock.getTimeStamp().subtract(parentBlock.getTimeStamp());
		return calculateBlockScoreImpl(timeDiff, currentBlock.getDifficulty().getRaw());
	}

	private long calculateBlockScoreImpl(int timeDiff, long difficulty) {
		return difficulty - timeDiff;
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

		long oldDifficulty = difficulties.get(difficulties.size()-1).getRaw();
		if (19L * oldDifficulty > 20L * difficulty) {
			difficulty = (19L * oldDifficulty)/20L;
		} else {
			if (21L * oldDifficulty < 20L * difficulty) {
				difficulty = (21L * oldDifficulty)/20L;
			}
		}

		return new BlockDifficulty(difficulty);
	}
}
