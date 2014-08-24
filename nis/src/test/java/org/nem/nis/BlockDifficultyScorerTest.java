package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.BlockDifficulty;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class BlockDifficultyScorerTest {
	public static final BlockDifficulty D = BlockDifficulty.INITIAL_DIFFICULTY;
	public static final int T = 60; // target time between blocks

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
		Assert.assertThat(blockDifficulty1, IsEqual.equalTo(D));
		Assert.assertThat(blockDifficulty2, IsEqual.equalTo(D));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf60sChangesTheDiffInOldBlockScorer() {
		// Arrange:
		final BlockDifficulty blockDifficulty = this.getBlockDifficultyVariableTimeOld(T);

		// Assert:
		Assert.assertThat(blockDifficulty, IsNot.not(IsEqual.equalTo(D)));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf60sShouldntChangeTheDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = this.getBlockDifficultyVariableTime(T);

		// Assert:
		Assert.assertThat(blockDifficulty, IsEqual.equalTo(D));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf61sShouldDecreaseDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = this.getBlockDifficultyVariableTime(T + 1);

		// Assert:
		Assert.assertThat(blockDifficulty.compareTo(D), IsEqual.equalTo(-1));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf59sShouldIncreaseDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = this.getBlockDifficultyVariableTime((T - 1));

		// Assert:
		Assert.assertThat(blockDifficulty.compareTo(D), IsEqual.equalTo(1));
	}

	private BlockDifficulty getBlockDifficultyVariableTime(final int time) {
		return this.getBlockDifficultyVariableTimeAtHeight(time, BlockMarkerConstants.DIFFICULTY_FIX_HEIGHT + 100);
	}

	private BlockDifficulty getBlockDifficultyVariableTimeOld(final int time) {
		return this.getBlockDifficultyVariableTimeAtHeight(time, 100);
	}

	private BlockDifficulty getBlockDifficultyVariableTimeAtHeight(final int time, final long height) {
		final BlockDifficultyScorer blockDifficultyScorer = new BlockDifficultyScorer();
		final int ELEMENTS = 10;
		final List<BlockDifficulty> blockDifficulties = new ArrayList<>(ELEMENTS);
		final List<TimeInstant> timeInstants = new ArrayList<>(ELEMENTS);
		final int firstTime = 12345;
		for (int i = 0; i < ELEMENTS; ++i) {
			blockDifficulties.add(D);
			timeInstants.add(new TimeInstant(firstTime + i * time));
		}

		// Act:
		return blockDifficultyScorer.calculateDifficulty(blockDifficulties, timeInstants, height);
	}
}