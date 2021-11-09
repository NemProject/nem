package org.nem.nis;

import org.nem.core.model.NemGlobals;
import org.nem.core.model.primitive.BlockDifficulty;
import org.nem.core.time.TimeInstant;

import java.math.BigInteger;
import java.util.List;

/**
 * Strategy for calculating block difficulties.
 */
public class BlockDifficultyScorer {

	/**
	 * Calculates the difficulty based the last n blocks.
	 *
	 * @param difficulties Historical difficulties.
	 * @param timeStamps Historical timestamps.
	 * @return The difficulty for the next block.
	 */
	public BlockDifficulty calculateDifficulty(final List<BlockDifficulty> difficulties, final List<TimeInstant> timeStamps) {
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

		final long targetTimeBetweenBlocks = NemGlobals.getBlockChainConfiguration().getBlockGenerationTargetTime();
		long difficulty = BigInteger.valueOf(averageDifficulty).multiply(BigInteger.valueOf(targetTimeBetweenBlocks))
				.multiply(BigInteger.valueOf(heightDiff - 1)).divide(BigInteger.valueOf(timeDiff)).longValue();

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
