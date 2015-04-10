package org.nem.core.node;

import java.util.*;

/**
 * An enumeration of optional features that a node can support.
 */
public enum NodeFeature {

	/**
	 * The node supports looking up transactions by hash.
	 */
	TRANSACTION_HASH_LOOKUP(0x00000001),

	/**
	 * The node supports supplying historical account data.
	 */
	HISTORICAL_ACCOUNT_DATA(0x0000002),

	/**
	 * A placeholder value (should be replaced when there is another feature).
	 */
	PLACEHOLDER2(0x0000004);

	private final int value;

	NodeFeature(final Integer value) {
		this.value = value;
	}

	/**
	 * Gets the underlying value.
	 *
	 * @return The value.
	 */
	public int value() {
		return this.value;
	}

	/**
	 * Gets a node feature given a string representation.
	 *
	 * @param status The string representation.
	 * @return The node feature.
	 */
	public static NodeFeature fromString(final String status) {
		final NodeFeature features = valueOf(status);
		if (null == features) {
			throw new IllegalArgumentException(String.format("Invalid node feature: '%s'", status));
		}

		return features;
	}

	/**
	 * Merges multiple node features into a single integer (bitmask).
	 *
	 * @param featureArray The features to join.
	 * @return The joined features.
	 */
	public static int or(final NodeFeature... featureArray) {
		int featureBits = 0;
		for (final NodeFeature features : featureArray) {
			featureBits |= features.value();
		}

		return featureBits;
	}

	/**
	 * Explodes an integer feature bitmask into its component node features.
	 *
	 * @param featureBits The bitmask to explode.
	 * @return The node features.
	 */
	public static NodeFeature[] explode(final int featureBits) {
		final List<NodeFeature> features = new ArrayList<>();
		for (final NodeFeature feature : values()) {
			if (0 != (feature.value & featureBits)) {
				features.add(feature);
			}
		}

		return features.toArray(new NodeFeature[features.size()]);
	}
}
