package org.nem.specific.deploy;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

import java.util.*;

public class BlockChainFeatureTest {

	//region fromString

	@Test
	public void fromStringCanParseValidBlockChainFeaturesStringRepresentation() {
		// Arrange:
		final Map<String, BlockChainFeature> expectedMappings = new HashMap<String, BlockChainFeature>() {
			{
				this.put("PROOF_OF_IMPORTANCE", BlockChainFeature.PROOF_OF_IMPORTANCE);
				this.put("PROOF_OF_STAKE", BlockChainFeature.PROOF_OF_STAKE);
			}
		};

		// Act:
		for (final Map.Entry<String, BlockChainFeature> entry : expectedMappings.entrySet()) {
			final BlockChainFeature feature = BlockChainFeature.fromString(entry.getKey());

			// Assert:
			Assert.assertThat(feature, IsEqual.equalTo(entry.getValue()));
		}

		// Assert:
		Assert.assertThat(expectedMappings.size(), IsEqual.equalTo(BlockChainFeature.values().length));
	}

	@Test
	public void fromStringCannotParseInvalidBlockChainFeaturesStringRepresentation() {
		// Act:
		ExceptionAssert.assertThrows(
				v -> BlockChainFeature.fromString("BLAH"),
				IllegalArgumentException.class);
	}

	//endregion

	//region value / or / explode

	@Test
	public void valueReturnsUnderlyingValue() {
		// Act:
		final BlockChainFeature feature = BlockChainFeature.PROOF_OF_IMPORTANCE;

		// Assert:
		Assert.assertThat(feature.value(), IsEqual.equalTo(1));
	}

	@Test
	public void canBitwiseOrTogetherZeroFeatures() {
		// Act:
		final int value = BlockChainFeature.or();

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(0));
	}

	@Test
	public void canBitwiseOrTogetherSingleFeature() {
		// Act:
		final int value = BlockChainFeature.or(BlockChainFeature.PROOF_OF_IMPORTANCE);

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(1));
	}

	@Test
	public void canBitwiseOrTogetherMultipleFeatures() {
		// Act:
		// TODO: change this once there are more features (same applies to canExplodeMultipleFeatures)
		final int value = BlockChainFeature.or(BlockChainFeature.PROOF_OF_IMPORTANCE, BlockChainFeature.PROOF_OF_STAKE);

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(3));
	}

	@Test
	public void canExplodeZeroFeatures() {
		// Act:
		final BlockChainFeature[] features = BlockChainFeature.explode(0);

		// Assert:
		Assert.assertThat(features, IsEqual.equalTo(new BlockChainFeature[] {}));
	}

	@Test
	public void canExplodeOneFeature() {
		// Act:
		final BlockChainFeature[] features = BlockChainFeature.explode(2);

		// Assert:
		Assert.assertThat(features, IsEqual.equalTo(new BlockChainFeature[] { BlockChainFeature.PROOF_OF_STAKE }));
	}

	@Test
	public void canExplodeMultipleFeatures() {
		// Act:
		final BlockChainFeature[] features = BlockChainFeature.explode(3);

		// Assert:
		Assert.assertThat(features, IsEqual.equalTo(new BlockChainFeature[] { BlockChainFeature.PROOF_OF_IMPORTANCE, BlockChainFeature.PROOF_OF_STAKE }));
	}

	//endregion
}
