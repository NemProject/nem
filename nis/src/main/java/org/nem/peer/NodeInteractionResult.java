package org.nem.peer;

import org.nem.core.model.ValidationResult;
import org.nem.nis.sync.ComparisonResult;

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
			case SUCCESS: return NodeInteractionResult.SUCCESS;
			case NEUTRAL: 
			case FAILURE_ENTITY_UNUSABLE: // happens during initial sync or when one of the partners is on a fork
				return NodeInteractionResult.NEUTRAL;
			default: return NodeInteractionResult.FAILURE;
		}
	}

	/**
	 * Creates a new NodeInteractionResult from a comparison result code.
	 *
	 * @param comparisonResultCode The comparison result code.
	 * @return The NodeInteractionResult.
	 */
	public static NodeInteractionResult fromComparisonResultCode(final ComparisonResult.Code comparisonResultCode) {
		switch (comparisonResultCode) {
			case REMOTE_IS_SYNCED:
			case REMOTE_IS_TOO_FAR_BEHIND:
			case REMOTE_REPORTED_EQUAL_CHAIN_SCORE:
			case REMOTE_REPORTED_LOWER_CHAIN_SCORE:
				return NodeInteractionResult.NEUTRAL;
		}

		return NodeInteractionResult.FAILURE;
	}
}