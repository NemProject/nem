package org.nem.nis;

import org.nem.core.model.primitive.BlockDifficulty;
import org.nem.core.time.TimeInstant;
import org.nem.nis.secret.BlockChainConstants;

import java.math.BigInteger;
import java.util.List;

/**
 * Strategy for calculating block difficulties.
 *
 * TODO-CR: 20140808 - we really should test this class
 */
public class BlockDifficultyScorer {

	/**
	 * The target time between two blocks in seconds.
	 */
	private static final long TARGET_TIME_BETWEEN_BLOCKS = 86400L / BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;

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
