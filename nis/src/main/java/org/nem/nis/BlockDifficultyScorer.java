package org.nem.nis;

import org.nem.core.model.BlockChainConstants;
import org.nem.core.model.primitive.BlockDifficulty;
import org.nem.core.time.TimeInstant;

import java.math.BigInteger;
import java.util.List;

/**
 * Strategy for calculating block difficulties.
 */
public class BlockDifficultyScorer {

	/**
	 * The target time between two blocks in seconds.
	 */
	private static final long TARGET_TIME_BETWEEN_BLOCKS = 86400L / BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;

	/**
	 * Calculates the difficulty based the last n blocks.
	 *
	 * @param difficulties Historical difficulties.
	 * @param timeStamps Historical timestamps.
	 * @param height Height for which we're calculating difficulty.
	 * @return The difficulty for the next block.
	 */
	public BlockDifficulty calculateDifficulty(final List<BlockDifficulty> difficulties, final List<TimeInstant> timeStamps, final long height) {
		if (difficulties.size() < 2) {
			return BlockDifficulty.INITIAL_DIFFICULTY;
		}

		final TimeInstant newestTimeStamp = timeStamps.get(timeStamps.size() - 1);
		final TimeInstant oldestTimeStamp = timeStamps.get(0);
		final long timeDiff = newestTimeStamp.subtract(oldestTimeStamp);
		final long heightDiff = difficulties.size();
		long averageDifficulty = 0;
		for (final BlockDifficulty diff : difficulties) {
			averageDifficulty += diff.getRaw();
		}

		averageDifficulty /= heightDiff;

		long difficulty = BigInteger.valueOf(averageDifficulty).multiply(BigInteger.valueOf(TARGET_TIME_BETWEEN_BLOCKS))
				.multiply(BigInteger.valueOf(heightDiff - 1))
				.divide(BigInteger.valueOf(timeDiff))
				.longValue();

		final long oldDifficulty = difficulties.get(difficulties.size() - 1).getRaw();
		if (19L * oldDifficulty > 20L * difficulty) {
			difficulty = (19L * oldDifficulty) / 20L;
		} else {
			if (21L * oldDifficulty < 20L * difficulty) {
				difficulty = (21L * oldDifficulty) / 20L;
			}
		}

		return new BlockDifficulty(difficulty);
	}
}
