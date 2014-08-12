package org.nem.core.model;

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
	FAILURE_CHAIN_INVALID(13);

	private int value;

	private ValidationResult(int value) {
		this.value = value;
	}

	/**
	 * Creates a validation result given a raw value.
	 *
	 * @param value The value.
	 * @return The validation result if the value is known.
	 * @throws IllegalArgumentException if the value is unknown.
	 */
	public static ValidationResult fromValue(int value) {
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
}
