package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.ed25519.arithmetic.Ed25519GroupElement;

/**
 * Class that contains precomputed group element.
 */
public class Ed25519PrecomputedTable {

	/**
	 * Precomputed table for the base point which is used to speed up a single scalar multiplication.
	 */
	public static final Ed25519GroupElement[][] precomputedForSingle =
			Ed25519GroupElement.precomputeForScalarMultiplication(Ed25519Constants.curve, Ed25519Constants.basePoint);

	/**
	 * Precomputed table for the base point which is used to speed up a double scalar multiplication.
	 */
	public static final Ed25519GroupElement[] precomputedForDouble =
			Ed25519GroupElement.precomputeForDoubleScalarMultiplication(Ed25519Constants.curve, Ed25519Constants.basePoint);
}
