package org.nem.peer;

/**
 * Possible node interaction results.
 */
public enum NodeInteractionResult {
	/**
	 * Flag indicating that the experience was neutral.
	 */
	NEUTRAL,

	/**
	 * Flag indicating that the experience was good.
	 */
	SUCCESS,

	/**
	 * Flag indicating that the experience was bad.
	 */
	FAILURE
}