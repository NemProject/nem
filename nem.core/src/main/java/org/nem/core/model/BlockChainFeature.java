package org.nem.core.model;

import java.util.*;

/**
 * An enumeration of block chain features.
 */
public enum BlockChainFeature {

	/**
	 * The block chain uses the proof of importance consensus algorithm.
	 */
	PROOF_OF_IMPORTANCE(0x00000001),

	/**
	 * The block chain uses the proof of stake consensus algorithm.
	 */
	PROOF_OF_STAKE(0x00000002),

	/**
	 * Gaps between blocks should be more stable.
	 */
	STABILIZE_BLOCK_TIMES(0x00000004);

	private final int value;

	BlockChainFeature(final Integer value) {
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
	 * Gets a block chain feature given a string representation.
	 *
	 * @param status The string representation.
	 * @return The block chain feature.
	 */
	public static BlockChainFeature fromString(final String status) {
		final BlockChainFeature feature = valueOf(status);
		if (null == feature) {
			throw new IllegalArgumentException(String.format("Invalid block chain feature: '%s'", status));
		}

		return feature;
	}

	/**
	 * Merges multiple block chain features into a single integer (bitmask).
	 *
	 * @param featureArray The features to join.
	 * @return The joined features.
	 */
	public static int or(final BlockChainFeature... featureArray) {
		int featureBits = 0;
		for (final BlockChainFeature features : featureArray) {
			featureBits |= features.value();
		}

		return featureBits;
	}

	/**
	 * Explodes an integer feature bitmask into its component block chain features.
	 *
	 * @param featureBits The bitmask to explode.
	 * @return The block chain features.
	 */
	public static BlockChainFeature[] explode(final int featureBits) {
		final List<BlockChainFeature> features = new ArrayList<>();
		for (final BlockChainFeature feature : values()) {
			if (0 != (feature.value & featureBits)) {
				features.add(feature);
			}
		}

		return features.toArray(new BlockChainFeature[features.size()]);
	}
}
