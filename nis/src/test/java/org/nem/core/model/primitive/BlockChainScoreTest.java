package org.nem.core.model.primitive;

import net.minidev.json.JSONObject;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public class BlockChainScoreTest {

	//region constructor

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedAroundNegativeScore() {
		// Act:
		new BlockChainScore(-1);
	}

	@Test
	public void canBeCreatedAroundZeroScore() {
		// Act:
		final BlockChainScore score = new BlockChainScore(0);

		// Assert:
		Assert.assertThat(score.getRaw(), IsEqual.equalTo(0L));
	}

	@Test
	public void canBeCreatedAroundPositiveScore() {
		// Act:
		final BlockChainScore score = new BlockChainScore(1);

		// Assert:
		Assert.assertThat(score.getRaw(), IsEqual.equalTo(1L));
	}

	//endregion

	//region add/subtract

	@Test
	public void scoresCanBeAdded() {
		// Arrange:
		final BlockChainScore score1 = new BlockChainScore(17);
		final BlockChainScore score2 = new BlockChainScore(3);

		// Act:
		final BlockChainScore result = score1.add(score2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new BlockChainScore(20)));
	}

	@Test
	public void scoresCanBeSubtracted() {
		// Arrange:
		final BlockChainScore score1 = new BlockChainScore(17);
		final BlockChainScore score2 = new BlockChainScore(3);

		// Act:
		final BlockChainScore result = score1.subtract(score2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new BlockChainScore(14)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotSubtractLargerScoreFromSmallerScore() {
		// Arrange:
		final BlockChainScore score1 = new BlockChainScore(17);
		final BlockChainScore score2 = new BlockChainScore(3);

		// Act:
		score2.subtract(score1);
	}

	//endregion

	//region serialization

	@Test
	public void scoreCanBeSerialized() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final BlockChainScore score = new BlockChainScore(142);

		// Act:
		score.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(1));
		Assert.assertThat(jsonObject.get("score"), IsEqual.equalTo(142L));
	}

	@Test
	public void scoreCanBeRoundTripped() {
		// Act:
		final BlockChainScore score = createRoundTrippedScore(new BlockChainScore(142));

		// Assert:
		Assert.assertThat(score, IsEqual.equalTo(new BlockChainScore(142)));
	}

	private static BlockChainScore createRoundTrippedScore(final BlockChainScore originalScore) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalScore, null);
		return new BlockChainScore(deserializer);
	}

}
