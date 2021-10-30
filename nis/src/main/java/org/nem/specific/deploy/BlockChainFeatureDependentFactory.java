package org.nem.specific.deploy;

import org.nem.core.model.*;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Helper class for creating feature-dependent objects.
 */
public class BlockChainFeatureDependentFactory {

	/**
	 * Creates an object corresponding to the configured feature.
	 *
	 * @param <T> The object type.
	 * @param config The configuration.
	 * @param name The friendly object name.
	 * @param featureSupplierMap The feature to supplier map.
	 * @return The created object.
	 */
	public static <T> T createObject(final BlockChainConfiguration config, final String name,
			final Map<BlockChainFeature, Supplier<T>> featureSupplierMap) {
		T result = null;
		for (final Map.Entry<BlockChainFeature, Supplier<T>> entry : featureSupplierMap.entrySet()) {
			if (!config.isBlockChainFeatureSupported(entry.getKey())) {
				continue;
			}

			if (result != null) {
				throw new NisConfigurationException(String.format("multiple %ss configured", name));
			}

			result = entry.getValue().get();
		}

		if (null != result) {
			return result;
		}

		throw new NisConfigurationException(String.format("no valid %s configured", name));
	}
}
