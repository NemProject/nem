package org.nem.specific.deploy;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNull;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;
import java.util.function.Supplier;

public class BlockChainFeatureDependentFactoryTest {

	@Test
	public void canCreateObjectIfSingleMatchingFeatureIsSelected() {
		// Act:
		final Object object = createObject(BlockChainFeature.explode(7), BlockChainFeature.explode(2));

		// Assert:
		MatcherAssert.assertThat(object, IsNull.notNullValue());
	}

	@Test
	public void cannotCreateObjectIfMultipleMatchingFeaturesAreSelected() {
		// Act:
		ExceptionAssert.assertThrows(v -> createObject(BlockChainFeature.explode(7), BlockChainFeature.explode(5)),
				NisConfigurationException.class);
	}

	@Test
	public void cannotCreateObjectIfNoMatchingFeaturesAreSelected() {
		// Act:
		ExceptionAssert.assertThrows(v -> createObject(BlockChainFeature.explode(5), BlockChainFeature.explode(2)),
				NisConfigurationException.class);
	}

	private static Object createObject(final BlockChainFeature[] configuredFeatures, final BlockChainFeature[] matchingFeatures) {
		// Arrange:
		final BlockChainConfiguration config = new BlockChainConfigurationBuilder().setBlockChainFeatures(configuredFeatures).build();

		final Map<BlockChainFeature, Supplier<Object>> featureSupplierMap = new HashMap<>();
		Arrays.stream(matchingFeatures).forEach(feature -> featureSupplierMap.put(feature, Object::new));

		// Act:
		return BlockChainFeatureDependentFactory.createObject(config, "test object", featureSupplierMap);
	}
}
