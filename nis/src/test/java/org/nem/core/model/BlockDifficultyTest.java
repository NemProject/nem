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
				IsEqual.equalTo(new BlockDifficulty(50_000_000_000_000L)));
	}

	//endregion

	//region constructor

	@Test
	public void valueInDifficultyRangeIsNotChanged() {
		// Arrange:
		final BlockDifficulty difficulty = new BlockDifficulty(79_876_543_211_237L);

		// Assert:
		Assert.assertThat(difficulty.getRaw(), IsEqual.equalTo(79_876_543_211_237L));
	}

	@Test
	public void valueBelowMinDifficultyIsIncreasedToMinDifficulty() {
		// Arrange:
		final BlockDifficulty difficulty = new BlockDifficulty(1_654_321_123_000L);

		// Assert:
		Assert.assertThat(difficulty.getRaw(), IsEqual.equalTo(5_000_000_000_000L));
	}

	@Test
	public void valueAboveMaxDifficultyIsDecreasedToMaxDifficulty() {
		// Arrange:
		final BlockDifficulty difficulty = new BlockDifficulty(987_654_321_123_000L);

		// Assert:
		Assert.assertThat(difficulty.getRaw(), IsEqual.equalTo(500_000_000_000_000L));
	}

	//endregion

	@Test
	public void valueCanBeReturnedAsBigInteger() {
		// Arrange:
		final BlockDifficulty difficulty = new BlockDifficulty(79_876_543_211_237L);

		// Assert:
		Assert.assertThat(difficulty.asBigInteger(), IsEqual.equalTo(new BigInteger("79876543211237")));
	}
}
