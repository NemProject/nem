package org.nem.core.node;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;

public class NodeFeatureTest {

	//region fromString

	@Test
	public void fromStringCanParseValidNodeFeaturesStringRepresentation() {
		// Arrange:
		final Map<String, NodeFeature> expectedMappings = new HashMap<String, NodeFeature>() {
			{
				this.put("TRANSACTION_HASH_LOOKUP", NodeFeature.TRANSACTION_HASH_LOOKUP);
				this.put("HISTORICAL_ACCOUNT_DATA", NodeFeature.HISTORICAL_ACCOUNT_DATA);
				this.put("PLACEHOLDER2", NodeFeature.PLACEHOLDER2);
			}
		};

		// Act:
		for (final Map.Entry<String, NodeFeature> entry : expectedMappings.entrySet()) {
			final NodeFeature feature = NodeFeature.fromString(entry.getKey());

			// Assert:
			Assert.assertThat(feature, IsEqual.equalTo(entry.getValue()));
		}

		// Assert:
		Assert.assertThat(expectedMappings.size(), IsEqual.equalTo(NodeFeature.values().length));
	}

	@Test
	public void fromStringCannotParseInvalidNodeFeaturesStringRepresentation() {
		// Act:
		ExceptionAssert.assertThrows(
				v -> NodeFeature.fromString("BLAH"),
				IllegalArgumentException.class);
	}

	//endregion

	//region value / or / explode

	@Test
	public void valueReturnsUnderlyingValue() {
		// Act:
		final NodeFeature feature = NodeFeature.TRANSACTION_HASH_LOOKUP;

		// Assert:
		Assert.assertThat(feature.value(), IsEqual.equalTo(1));
	}

	@Test
	public void canBitwiseOrTogetherZeroFeatures() {
		// Act:
		final int value = NodeFeature.or();

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(0));
	}

	@Test
	public void canBitwiseOrTogetherSingleFeature() {
		// Act:
		final int value = NodeFeature.or(NodeFeature.HISTORICAL_ACCOUNT_DATA);

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(2));
	}

	@Test
	public void canBitwiseOrTogetherMultipleFeatures() {
		// Act:
		final int value = NodeFeature.or(NodeFeature.TRANSACTION_HASH_LOOKUP, NodeFeature.PLACEHOLDER2);

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(5));
	}

	@Test
	public void canExplodeZeroFeatures() {
		// Act:
		final NodeFeature[] features = NodeFeature.explode(0);

		// Assert:
		Assert.assertThat(features, IsEqual.equalTo(new NodeFeature[] {}));
	}

	@Test
	public void canExplodeOneFeature() {
		// Act:
		final NodeFeature[] features = NodeFeature.explode(2);

		// Assert:
		Assert.assertThat(features, IsEqual.equalTo(new NodeFeature[] { NodeFeature.HISTORICAL_ACCOUNT_DATA }));
	}

	@Test
	public void canExplodeMultipleFeatures() {
		// Act:
		final NodeFeature[] features = NodeFeature.explode(5);

		// Assert:
		Assert.assertThat(features, IsEqual.equalTo(new NodeFeature[] { NodeFeature.TRANSACTION_HASH_LOOKUP, NodeFeature.PLACEHOLDER2 }));
	}

	//endregion
}