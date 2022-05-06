package org.nem.nis;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockDifficulty;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.logging.Logger;

public class BlockDifficultyScorerTest {
	private static final Logger LOGGER = Logger.getLogger(BlockDifficultyScorerTest.class.getName());

	private static final BlockDifficulty BASE_DIFF = BlockDifficulty.INITIAL_DIFFICULTY;
	private static final int TARGET_TIME = 60; // target time between blocks

	@Test
	public void calculatingDifficultyOnSmallListReturnsBaseDifficulty() {
		// Arrange:
		final BlockDifficultyScorer blockDifficultyScorer = new BlockDifficultyScorer();
		final List<BlockDifficulty> blockDifficulties1 = new ArrayList<>();
		final List<BlockDifficulty> blockDifficulties2 = Collections.singletonList(new BlockDifficulty(1));

		// Act:
		final BlockDifficulty blockDifficulty1 = blockDifficultyScorer.calculateDifficulty(blockDifficulties1, null);
		final BlockDifficulty blockDifficulty2 = blockDifficultyScorer.calculateDifficulty(blockDifficulties2, null);

		// Assert:
		MatcherAssert.assertThat(blockDifficulty1, IsEqual.equalTo(BASE_DIFF));
		MatcherAssert.assertThat(blockDifficulty2, IsEqual.equalTo(BASE_DIFF));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf60sShouldNotChangeTheDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = this.getBlockDifficultyVariableTime(TARGET_TIME);

		// Assert:
		MatcherAssert.assertThat(blockDifficulty, IsEqual.equalTo(BASE_DIFF));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf61sShouldDecreaseDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = this.getBlockDifficultyVariableTime(TARGET_TIME + 1);

		// Assert:
		MatcherAssert.assertThat(blockDifficulty.compareTo(BASE_DIFF), IsEqual.equalTo(-1));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf59sShouldIncreaseDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = this.getBlockDifficultyVariableTime((TARGET_TIME - 1));

		// Assert:
		MatcherAssert.assertThat(blockDifficulty.compareTo(BASE_DIFF), IsEqual.equalTo(1));
	}

	private BlockDifficulty getBlockDifficultyVariableTime(final int time) {
		return this.getBlockDifficultyVariableTimeAtHeight(time);
	}

	private BlockDifficulty getBlockDifficultyVariableTimeAtHeight(final int time) {
		final BlockDifficultyScorer blockDifficultyScorer = new BlockDifficultyScorer();
		final int ELEMENTS = 10;
		final List<BlockDifficulty> blockDifficulties = new ArrayList<>(ELEMENTS);
		final List<TimeInstant> timeInstants = new ArrayList<>(ELEMENTS);
		final int firstTime = 12345;
		for (int i = 0; i < ELEMENTS; ++i) {
			blockDifficulties.add(BASE_DIFF);
			timeInstants.add(new TimeInstant(firstTime + i * time));
		}

		// Act:
		return blockDifficultyScorer.calculateDifficulty(blockDifficulties, timeInstants);
	}

	// this is slightly different than the test above, as
	// we are adjusting difficulty as we go
	@Test
	public void whenTimeIsBelowTargetTimeDifficultyRaises() {
		assertDifficultyChangesDependingOnTimeChange(59, 1);
	}

	@Test
	public void whenTimeIsAboveTargetTimeDifficultyDecreases() {
		assertDifficultyChangesDependingOnTimeChange(61, -1);
	}

	private static void assertDifficultyChangesDependingOnTimeChange(final int timeNeededForGeneratingBlock, final int comparisonResult) {
		// Arrange:
		// - initial block difficulties: BASE_DIFF, BASE_DIFF
		// - initial time stamps: t, t + TIME_DIFF
		final TimeInstant initialTime = new TimeInstant(100);
		final BlockDifficultyScorer blockDifficultyScorer = new BlockDifficultyScorer();
		final List<BlockDifficulty> blockDifficulties = new ArrayList<>(Arrays.asList(BASE_DIFF, BASE_DIFF));
		final List<TimeInstant> timeInstants = new ArrayList<>(
				Arrays.asList(initialTime, initialTime.addSeconds(timeNeededForGeneratingBlock)));

		BlockDifficulty prevDifficulty = BASE_DIFF;
		for (int i = 0; i < 60; ++i) {
			// Act: calculate the difficulty using current information
			final BlockDifficulty diff = blockDifficultyScorer.calculateDifficulty(blockDifficulties, timeInstants);

			// Assert: the difficulty changed in the expected direction
			MatcherAssert.assertThat(diff.compareTo(prevDifficulty), IsEqual.equalTo(comparisonResult));

			// Arrange: update
			// - the difficulties (add current difficulty)
			// - time-instants (add previous + TIME_DIFF)
			// - previous difficulty (current difficulty)
			blockDifficulties.add(diff);
			timeInstants.add(timeInstants.get(timeInstants.size() - 1).addSeconds(timeNeededForGeneratingBlock));
			prevDifficulty = diff;
		}
	}

	@Test
	public void whenTimeIsMuchBelowTargetTimeDifficultyRaisesAtMostFivePercentPerBlock() {
		percentageChange(1, 2, 5L);
	}

	@Test
	public void whenTimeIsMuchAboveTargetTimeDifficultyDecreasesAtMostFivePercentPerBlock() {
		// it is ok that a larger difference (248 - 60) is required for a 5% decrease
		// than a 5% increase (60 - 2)
		percentageChange(-1, 248, -5L);
	}

	private static void percentageChange(final int adjustment, final int timeNeededForGeneratingBlock, final long expectedChange) {
		// Arrange:
		// - initial block difficulties: BASE_DIFF, BASE_DIFF
		// - initial time stamps: t, t + TIME_DIFF
		final TimeInstant initialTime = new TimeInstant(100);
		final BlockDifficultyScorer blockDifficultyScorer = new BlockDifficultyScorer();
		final List<BlockDifficulty> blockDifficulties = new ArrayList<>(Arrays.asList(BASE_DIFF, BASE_DIFF));
		final List<TimeInstant> timeInstants = new ArrayList<>(
				Arrays.asList(initialTime, initialTime.addSeconds(timeNeededForGeneratingBlock)));

		// Act + Assert
		BlockDifficulty prevDifficulty = blockDifficulties.get(1);
		for (int i = 0; i < 100; ++i) {
			// Act: calculate the difficulty using current information
			final BlockDifficulty diff = blockDifficultyScorer.calculateDifficulty(blockDifficulties, timeInstants);
			final long percentageChange = (diff.getRaw() - prevDifficulty.getRaw() + adjustment) * 100 / prevDifficulty.getRaw();

			if (isClamped(diff)) {
				LOGGER.info(String.format("difficulty is clamped after %d iterations", i));
				MatcherAssert.assertThat(String.format("breaking after %d iterations", i), i > 40, IsEqual.equalTo(true));
				break;
			}

			// Assert: the percentage change matches the expected change
			LOGGER.info(String.format("%d %s %s vs %s", i, percentageChange, diff, BASE_DIFF.getRaw() / 10));
			MatcherAssert.assertThat(percentageChange, IsEqual.equalTo(expectedChange));

			// Arrange: update
			// - the difficulties (add current difficulty)
			// - time-instants (add previous + TIME_DIFF)
			// - previous difficulty (current difficulty)
			blockDifficulties.add(diff);
			timeInstants.add(timeInstants.get(timeInstants.size() - 1).addSeconds(timeNeededForGeneratingBlock));
			prevDifficulty = diff;
		}
	}

	private static boolean isClamped(final BlockDifficulty diff) {
		return 0 == diff.compareTo(new BlockDifficulty(diff.getRaw() + 1)) || 0 == diff.compareTo(new BlockDifficulty(diff.getRaw() - 1));
	}

	@Test
	public void whenTimeIsBelowTargetTimeDifficultyDoesNotChangeWhenItReachesMaximum() {
		// Arrange:
		// - initial block difficulties: MAX_DIFF, MAX_DIFF
		// - initial time stamps: t, t + 2
		final TimeInstant initialTime = new TimeInstant(100);
		final BlockDifficulty maxDifficulty = new BlockDifficulty(Long.MAX_VALUE);
		final BlockDifficultyScorer blockDifficultyScorer = new BlockDifficultyScorer();
		final List<BlockDifficulty> blockDifficulties = new ArrayList<>(Arrays.asList(maxDifficulty, maxDifficulty));
		final List<TimeInstant> timeInstants = new ArrayList<>(Arrays.asList(initialTime, initialTime.addSeconds(2)));

		for (int i = 0; i < 100; ++i) {
			// Act: calculate the difficulty using current information
			final BlockDifficulty diff = blockDifficultyScorer.calculateDifficulty(blockDifficulties, timeInstants);

			// Assert: the difficulty does not change (it is clamped at max difficulty)
			MatcherAssert.assertThat(diff, IsEqual.equalTo(maxDifficulty));

			// Arrange: update
			// - the difficulties (add current difficulty)
			// - time-instants (add previous + 2)
			blockDifficulties.add(diff);
			timeInstants.add(timeInstants.get(timeInstants.size() - 1).addSeconds(2));
		}
	}
}
