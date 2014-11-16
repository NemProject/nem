package org.nem.core.model;

import java.util.Iterator;

/**
 * Possible validation results.
 */
public enum ValidationResult {

	/**
	 * Validation is neutral.
	 */
	NEUTRAL(0),

	/**
	 * Validation succeeded.
	 */
	SUCCESS(1),

	/**
	 * Validation failed for an unknown reason.
	 */
	FAILURE_UNKNOWN(2),

	/**
	 * Validation failed because the deadline passed.
	 */
	FAILURE_PAST_DEADLINE(3),

	/**
	 * Validation failed because the deadline is too far in the future.
	 */
	FAILURE_FUTURE_DEADLINE(4),

	/**
	 * Validation failed because the account had an insufficient balance.
	 */
	FAILURE_INSUFFICIENT_BALANCE(5),

	/**
	 * Validation failed because the message is too large.
	 */
	FAILURE_MESSAGE_TOO_LARGE(6),

	/**
	 * Validation failed because the transaction hash is already known.
	 */
	FAILURE_HASH_EXISTS(7),

	/**
	 * Validation failed because the verification of the signature failed.
	 */
	FAILURE_SIGNATURE_NOT_VERIFIABLE(8),

	/**
	 * Validation failed because the time stamp is too far in the past.
	 */
	FAILURE_TIMESTAMP_TOO_FAR_IN_PAST(9),

	/**
	 * Validation failed because the time stamp is too far in the future.
	 */
	FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE(10),

	/**
	 * Validation failed because the entity cannot be used for some reason.
	 */
	FAILURE_ENTITY_UNUSABLE(11),

	/**
	 * Validation failed because the chain score is inferior to our score.
	 */
	FAILURE_CHAIN_SCORE_INFERIOR(12),

	/**
	 * Validation failed because the chain could not be validated.
	 */
	FAILURE_CHAIN_INVALID(13),

	/**
	 * Validation failed because conflicting importance transfer is present
	 */
	FAILURE_CONFLICTING_IMPORTANCE_TRANSFER(14),

	/**
	 * Validation failed because there are too many transactions in a block.
	 */
	FAILURE_TOO_MANY_TRANSACTIONS(15),

	/**
	 * Validation failed because a block contained a self-signed transaction.
	 */
	FAILURE_SELF_SIGNED_TRANSACTION(16),

	/**
	 * Validation failed because remote harvesting account has non-zero balance.
	 */
	FAILURE_DESTINATION_ACCOUNT_HAS_NONZERO_BALANCE(17),

	/**
	 * Validation failed because previous importance transfer change is in progress.
	 */
	FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS(18),

	/**
	 * Validation failed because importance transfer activation was attempted while already activated.
	 */
	FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED(19),

	/**
	 * Validation failed because importance transfer deactivation was attempted while already deactivated.
	 */
	FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE(20);

	private final int value;

	private ValidationResult(final int value) {
		this.value = value;
	}

	/**
	 * Creates a validation result given a raw value.
	 *
	 * @param value The value.
	 * @return The validation result if the value is known.
	 * @throws IllegalArgumentException if the value is unknown.
	 */
	public static ValidationResult fromValue(final int value) {
		for (final ValidationResult result : values()) {
			if (result.getValue() == value) {
				return result;
			}
		}

		throw new IllegalArgumentException("Invalid validation result: " + value);
	}

	/**
	 * Gets a value indicating whether or not this result indicates success.
	 *
	 * @return true if this result indicates success.
	 */
	public boolean isSuccess() {
		return this == ValidationResult.SUCCESS;
	}

	/**
	 * Gets a value indicating whether or not this result indicates an error.
	 *
	 * @return true if this result indicates an error.
	 */
	public boolean isFailure() {
		switch (this) {
			case NEUTRAL:
				return false;
		}

		return !this.isSuccess();
	}

	/**
	 * Gets the underlying integer representation of the result.
	 *
	 * @return The underlying value.
	 */
	public int getValue() {
		return this.value;
	}

	/**
	 * Aggregates an iterator of validation results. This implementation short-circuits on the first failure.
	 *
	 * @param resultIterator The results to aggregate.
	 * @return The aggregated result.
	 */
	public static ValidationResult aggregate(final Iterator<ValidationResult> resultIterator) {
		boolean isNeutral = false;
		while (resultIterator.hasNext()) {
			final ValidationResult result = resultIterator.next();
			if (ValidationResult.NEUTRAL == result) {
				isNeutral = true;
			} else if (ValidationResult.SUCCESS != result) {
				return result;
			}
		}

		return isNeutral ? ValidationResult.NEUTRAL : ValidationResult.SUCCESS;
	}
}
