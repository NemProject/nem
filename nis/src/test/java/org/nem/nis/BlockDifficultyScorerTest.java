package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockDifficulty;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class BlockDifficultyScorerTest {
	public static final BlockDifficulty BASE_DIFF = BlockDifficulty.INITIAL_DIFFICULTY;
	public static final int TARGET_TIME = 60; // target time between blocks

	@Test
	public void calculatingDifficultyOnSmallListReturnsBaseDifficulty() {
		// Arrange:
		final BlockDifficultyScorer blockDifficultyScorer = new BlockDifficultyScorer();
		final List<BlockDifficulty> blockDifficulties1 = new ArrayList<>();
		final List<BlockDifficulty> blockDifficulties2 = Arrays.asList(new BlockDifficulty(1));

		// Act:
		final BlockDifficulty blockDifficulty1 = blockDifficultyScorer.calculateDifficulty(blockDifficulties1, null, 100);
		final BlockDifficulty blockDifficulty2 = blockDifficultyScorer.calculateDifficulty(blockDifficulties2, null, 100);

		// Assert:
		Assert.assertThat(blockDifficulty1, IsEqual.equalTo(BASE_DIFF));
		Assert.assertThat(blockDifficulty2, IsEqual.equalTo(BASE_DIFF));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf60sShouldNotChangeTheDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = this.getBlockDifficultyVariableTime(TARGET_TIME);

		// Assert:
		Assert.assertThat(blockDifficulty, IsEqual.equalTo(BASE_DIFF));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf61sShouldDecreaseDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = this.getBlockDifficultyVariableTime(TARGET_TIME + 1);

		// Assert:
		Assert.assertThat(blockDifficulty.compareTo(BASE_DIFF), IsEqual.equalTo(-1));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf59sShouldIncreaseDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = this.getBlockDifficultyVariableTime((TARGET_TIME - 1));

		// Assert:
		Assert.assertThat(blockDifficulty.compareTo(BASE_DIFF), IsEqual.equalTo(1));
	}

	private BlockDifficulty getBlockDifficultyVariableTime(final int time) {
		return this.getBlockDifficultyVariableTimeAtHeight(time, 100);
	}

	private BlockDifficulty getBlockDifficultyVariableTimeAtHeight(final int time, final long height) {
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
		return blockDifficultyScorer.calculateDifficulty(blockDifficulties, timeInstants, height);
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
		final BlockDifficultyScorer blockDifficultyScorer = new BlockDifficultyScorer();
		final List<BlockDifficulty> blockDifficulties = new ArrayList<>();
		final List<TimeInstant> timeInstants = new ArrayList<>();

		final int time = 100;
		blockDifficulties.add(BASE_DIFF);
		blockDifficulties.add(BASE_DIFF);
		timeInstants.add(new TimeInstant(time));
		timeInstants.add(new TimeInstant(time + timeNeededForGeneratingBlock));

		// Act + Assert
		BlockDifficulty prevDifficulty = BASE_DIFF;
		for (int i = 0; i < 60; ++i) {
			final BlockDifficulty diff = blockDifficultyScorer.calculateDifficulty(blockDifficulties, timeInstants, 0);
			Assert.assertThat(diff.compareTo(prevDifficulty), IsEqual.equalTo(comparisonResult));

			blockDifficulties.add(diff);
			// TODO 20150111 J-G: why (i+2) here?
			timeInstants.add(new TimeInstant(time + timeNeededForGeneratingBlock * (i + 2)));
			prevDifficulty = diff;
		}
	}

	@Test
	public void whenTimeIsMuchBelowTargetTimeDifficultyRaisesAtMostFivePercentPerBlock() {
		percentageChange(1, 2, 5L);
	}

	@Test
	public void whenTimeIsMuchAboveTargetTimeDifficultyDecreasesAtMostFivePercentPerBlock() {
		// TODO 20150109 G-BR: 248 is the lowest value that this test pass with
		// > should we allow bigger decrease, when going down?
		percentageChange(-1, 248, -5L);
	}

	private static void percentageChange(final int adjustment, final int timeNeededToGenerateABlock, final long expectedChange) {
		// Arrange:
		final BlockDifficultyScorer blockDifficultyScorer = new BlockDifficultyScorer();
		final List<BlockDifficulty> blockDifficulties = new ArrayList<>();
		final List<TimeInstant> timeInstants = new ArrayList<>();

		final int time = 100;
		blockDifficulties.add(BASE_DIFF);
		blockDifficulties.add(BASE_DIFF);
		timeInstants.add(new TimeInstant(time));
		timeInstants.add(new TimeInstant(time + timeNeededToGenerateABlock));

		// Act + Assert
		BlockDifficulty prevDifficulty = blockDifficulties.get(1);
		for (int i = 0; i < 100; ++i) {
			final BlockDifficulty diff = blockDifficultyScorer.calculateDifficulty(blockDifficulties, timeInstants, 0);

			// TODO 20150111 J-G: why are you breaking out of the loop? i think the test should be more deterministic
			if (diff.getRaw() >= (10 * BASE_DIFF.getRaw()) || diff.getRaw() <= (BASE_DIFF.getRaw() / 10)) {
				break;
			}

			final long percentageChange = (diff.getRaw() - prevDifficulty.getRaw() + adjustment) * 100 / prevDifficulty.getRaw();
			System.out.println(i + " " + percentageChange + " " + diff + " vs " + (BASE_DIFF.getRaw() / 10));
			Assert.assertThat(percentageChange, IsEqual.equalTo(expectedChange));

			blockDifficulties.add(diff);
			timeInstants.add(new TimeInstant(time + timeNeededToGenerateABlock * (i + 2)));
			prevDifficulty = diff;
		}
	}

	@Test
	public void whenTimeIsBelowTargetTimeDifficultyDoesNotChangeWhenItReachesMaximum() {
		// Arrange:
		final BlockDifficultyScorer blockDifficultyScorer = new BlockDifficultyScorer();
		final List<BlockDifficulty> blockDifficulties = new ArrayList<>();
		final List<TimeInstant> timeInstants = new ArrayList<>();

		final int time = 100;
		blockDifficulties.add(new BlockDifficulty(10 * BASE_DIFF.getRaw()));
		blockDifficulties.add(new BlockDifficulty(10 * BASE_DIFF.getRaw()));
		timeInstants.add(new TimeInstant(time));
		timeInstants.add(new TimeInstant(time + 2));

		// Act + Assert
		BlockDifficulty prevDifficulty = blockDifficulties.get(0);
		for (int i = 0; i < 100; ++i) {
			final BlockDifficulty diff = blockDifficultyScorer.calculateDifficulty(blockDifficulties, timeInstants, 0);
			Assert.assertThat(diff, IsEqual.equalTo(prevDifficulty));

			blockDifficulties.add(diff);
			// TODO 20150111 J-G: why 2 * (i+2)
			timeInstants.add(new TimeInstant(time + 2 * (i + 2)));
			prevDifficulty = diff;
		}
	}
}