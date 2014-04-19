package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;

import java.math.BigInteger;

public class BlockDifficultyTest {

	//region constants

	@Test
	public void constantsAreInitializedCorrectly() {
		// Assert:
		Assert.assertThat(
				BlockDifficulty.INITIAL_DIFFICULTY,
				IsEqual.equalTo(new BlockDifficulty(120_000_000_000L)));
	}

	//endregion

	//region constructor

	@Test
	public void valueInDifficultyRangeIsNotChanged() {
		// Arrange:
		final BlockDifficulty difficulty = new BlockDifficulty(987_654_321_123L);

		// Assert:
		Assert.assertThat(difficulty.getRaw(), IsEqual.equalTo(987_654_321_123L));
	}

	@Test
	public void valueBelowMinDifficultyIsIncreasedToMinDifficulty() {
		// Arrange:
		final BlockDifficulty difficulty = new BlockDifficulty(7_654_321_123L);

		// Assert:
		Assert.assertThat(difficulty.getRaw(), IsEqual.equalTo(12_000_000_000L));
	}

	@Test
	public void valueAboveMaxDifficultyIsDecreasedToMaxDifficulty() {
		// Arrange:
		final BlockDifficulty difficulty = new BlockDifficulty(2_987_654_321_123L);

		// Assert:
		Assert.assertThat(difficulty.getRaw(), IsEqual.equalTo(1_200_000_000_000L));
	}

	//endregion

	@Test
	public void valueCanBeReturnedAsBigInteger() {
		// Arrange:
		final BlockDifficulty difficulty = new BlockDifficulty(987_654_321_123L);

		// Assert:
		Assert.assertThat(difficulty.asBigInteger(), IsEqual.equalTo(new BigInteger("987654321123")));
	}
}
