package org.nem.nis;

import org.hamcrest.core.IsEqual;
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
		final BlockDifficulty blockDifficulty1 = blockDifficultyScorer.calculateDifficulty(blockDifficulties1, null);
		final BlockDifficulty blockDifficulty2 = blockDifficultyScorer.calculateDifficulty(blockDifficulties2, null);

		// Assert:
		Assert.assertThat(blockDifficulty1, IsEqual.equalTo(D));
		Assert.assertThat(blockDifficulty2, IsEqual.equalTo(D));
	}

	@Test
	// TODO 20140820: G->B: is this test wrong or is our code wrong?
	public void blocksWithInitialDiffAndTimeDiffOf60sShouldntChangeTheDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = getBlockDifficultyVariableTime(T);

		// Assert:
		Assert.assertThat(blockDifficulty, IsEqual.equalTo(D));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf61sShouldDecreaseDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = getBlockDifficultyVariableTime(T + 1);

		// Assert:
		Assert.assertThat(blockDifficulty.compareTo(D), IsEqual.equalTo(-1));
	}

	@Test
	public void blocksWithInitialDiffAndTimeDiffOf59sShouldIncreaseDiff() {
		// Arrange:
		final BlockDifficulty blockDifficulty = getBlockDifficultyVariableTime((T - 1));

		// Assert:
		Assert.assertThat(blockDifficulty.compareTo(D), IsEqual.equalTo(1));
	}

	private BlockDifficulty getBlockDifficultyVariableTime(int time) {
		final BlockDifficultyScorer blockDifficultyScorer = new BlockDifficultyScorer();
		final int ELEMENTS = 10;
		final List<BlockDifficulty> blockDifficulties = new ArrayList<>(ELEMENTS);
		final List<TimeInstant> timeInstants = new ArrayList<>(ELEMENTS);
		int firstTime = 12345;
		for (int i = 0; i < ELEMENTS; ++i) {
			blockDifficulties.add(D);
			timeInstants.add(new TimeInstant(firstTime + i * time));
		}

		// Act:
		return blockDifficultyScorer.calculateDifficulty(blockDifficulties, timeInstants);
	}
}