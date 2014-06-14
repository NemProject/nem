package org.nem.core.model.primitive;

import java.math.BigInteger;

import net.minidev.json.JSONObject;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;
import org.nem.core.utils.Base64Encoder;

public class BlockChainScoreTest {

	//region constructor

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedAroundNegativeScore() {
		// Act:
		new BlockChainScore(BigInteger.valueOf(-1));
	}

	@Test
	public void canBeCreatedAroundZeroScore() {
		// Act:
		final BlockChainScore score = new BlockChainScore(BigInteger.ZERO);

		// Assert:
		Assert.assertThat(score.getRaw(), IsEqual.equalTo(BigInteger.ZERO));
	}

	@Test
	public void canBeCreatedAroundPositiveScore() {
		// Act:
		final BlockChainScore score = new BlockChainScore(BigInteger.ONE);

		// Assert:
		Assert.assertThat(score.getRaw(), IsEqual.equalTo(BigInteger.ONE));
	}

	//endregion

	//region add/subtract

	@Test
	public void scoresCanBeAdded() {
		// Arrange:
		final BlockChainScore score1 = new BlockChainScore(BigInteger.valueOf(17));
		final BlockChainScore score2 = new BlockChainScore(BigInteger.valueOf(3));

		// Act:
		final BlockChainScore result = score1.add(score2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new BlockChainScore(BigInteger.valueOf(20))));
	}

	@Test
	public void scoresCanBeSubtracted() {
		// Arrange:
		final BlockChainScore score1 = new BlockChainScore(BigInteger.valueOf(17));
		final BlockChainScore score2 = new BlockChainScore(BigInteger.valueOf(3));

		// Act:
		final BlockChainScore result = score1.subtract(score2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new BlockChainScore(BigInteger.valueOf(14))));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotSubtractLargerScoreFromSmallerScore() {
		// Arrange:
		final BlockChainScore score1 = new BlockChainScore(BigInteger.valueOf(17));
		final BlockChainScore score2 = new BlockChainScore(BigInteger.valueOf(3));

		// Act:
		score2.subtract(score1);
	}

	//endregion

	//region serialization

	@Test
	public void scoreCanBeSerialized() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final BlockChainScore score = new BlockChainScore(BigInteger.valueOf(142));

		// Act:
		score.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(1));
		Assert.assertThat(jsonObject.get("score"), IsEqual.equalTo(Base64Encoder.getString(BigInteger.valueOf(142).toByteArray())));
	}

	@Test
	public void scoreCanBeRoundTripped() {
		// Act:
		final BlockChainScore score = createRoundTrippedScore(new BlockChainScore(BigInteger.valueOf(142)));

		// Assert:
		Assert.assertThat(score, IsEqual.equalTo(new BlockChainScore(BigInteger.valueOf(142))));
	}

	private static BlockChainScore createRoundTrippedScore(final BlockChainScore originalScore) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalScore, null);
		return new BlockChainScore(deserializer);
	}

}
