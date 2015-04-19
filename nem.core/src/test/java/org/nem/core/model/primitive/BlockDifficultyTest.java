package org.nem.core.model.primitive;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

import java.math.BigInteger;

public class BlockDifficultyTest {

	//region constants

	@Test
	public void constantsAreInitializedCorrectly() {
		// Assert:
		Assert.assertThat(
				BlockDifficulty.INITIAL_DIFFICULTY,
				IsEqual.equalTo(new BlockDifficulty(100_000_000_000_000L)));
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
		Assert.assertThat(difficulty.getRaw(), IsEqual.equalTo(10_000_000_000_000L));
	}

	@Test
	public void valueAboveMaxDifficultyIsDecreasedToMaxDifficulty() {
		// Arrange:
		final BlockDifficulty difficulty = new BlockDifficulty(1_987_654_321_123_000L);

		// Assert:
		Assert.assertThat(difficulty.getRaw(), IsEqual.equalTo(1_000_000_000_000_000L));
	}

	//endregion

	//region converters

	@Test
	public void valueCanBeReturnedAsBigInteger() {
		// Arrange:
		final BlockDifficulty difficulty = new BlockDifficulty(79_876_543_211_237L);

		// Assert:
		Assert.assertThat(difficulty.asBigInteger(), IsEqual.equalTo(new BigInteger("79876543211237")));
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteBlockDifficulty() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final BlockDifficulty difficulty = new BlockDifficulty(79_876_543_211_237L);

		// Act:
		BlockDifficulty.writeTo(serializer, "difficulty", difficulty);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("difficulty"), IsEqual.equalTo(79_876_543_211_237L));
	}

	@Test
	public void canRoundtripBlockHeight() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final BlockDifficulty originalDifficulty = new BlockDifficulty(79_876_543_211_237L);

		// Act:
		BlockDifficulty.writeTo(serializer, "difficulty", originalDifficulty);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final BlockDifficulty difficulty = BlockDifficulty.readFrom(deserializer, "difficulty");

		// Assert:
		Assert.assertThat(difficulty, IsEqual.equalTo(originalDifficulty));
	}

	//endregion
}
