package org.nem.peer;

import org.nem.core.model.ValidationResult;

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
	FAILURE;

	/**
	 * Creates a new NodeInteractionResult from a ValidationResult.
	 *
	 * @param validationResult The ValidationResult.
	 * @return The NodeInteractionResult.
	 */
	public static NodeInteractionResult fromValidationResult(final ValidationResult validationResult) {
		switch (validationResult) {
			case SUCCESS:
				return NodeInteractionResult.SUCCESS;

			case NEUTRAL:

			case FAILURE_ENTITY_UNUSABLE_OUT_OF_SYNC:
				// happens during initial sync or when one of the partners is on a fork

			case FAILURE_TRANSACTION_CACHE_TOO_FULL:
				// happens when an account tries to place more transactions into the cache than its fair share
				return NodeInteractionResult.NEUTRAL;

			default :
				return NodeInteractionResult.FAILURE;
		}
	}
}
