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
	 * Validation failed because the entity cannot be used because the nodes are out of sync.
	 */
	FAILURE_ENTITY_UNUSABLE_OUT_OF_SYNC(12),

	/**
	 * Validation failed because the chain score is inferior to our score.
	 */
	FAILURE_CHAIN_SCORE_INFERIOR(13),

	/**
	 * Validation failed because the chain could not be validated.
	 */
	FAILURE_CHAIN_INVALID(14),

	/**
	 * Validation failed because conflicting importance transfer is present
	 */
	FAILURE_CONFLICTING_IMPORTANCE_TRANSFER(15),

	/**
	 * Validation failed because there are too many transactions in a block.
	 */
	FAILURE_TOO_MANY_TRANSACTIONS(16),

	/**
	 * Validation failed because a block contained a self-signed transaction.
	 */
	FAILURE_SELF_SIGNED_TRANSACTION(17),

	/**
	 * Validation failed because a transaction has an insufficient fee.
	 */
	FAILURE_INSUFFICIENT_FEE(18),

	// TODO 20150126 J-G: looks like this and the previous are both 18; what's the best way to change them without breaking NCC?
	/**
	 * Validation failed because remote harvesting account has a pre-existing balance transfer.
	 */
	FAILURE_DESTINATION_ACCOUNT_HAS_PREEXISTING_BALANCE_TRANSFER(18),

	/**
	 * Validation failed because previous importance transfer change is in progress.
	 */
	FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS(19),

	/**
	 * Validation failed because importance transfer activation was attempted while already activated.
	 */
	FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED(20),

	/**
	 * Validation failed because importance transfer deactivation was attempted while already deactivated.
	 */
	FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE(21),

	/**
	 * Validation failed because signer is not a cosigner of given multisig account.
	 */
	FAILURE_MULTISIG_NOT_A_COSIGNER(30),

	/**
	 * Validation failed because the cosignatories attached to a multisig transaction were invalid.
	 */
	FAILURE_MULTISIG_INVALID_COSIGNERS(31),

	/**
	 * Validation failed because a multisig signature was not associated with any known multisig transaction.
	 */
	FAILURE_MULTISIG_NO_MATCHING_MULTISIG(32),

	/**
	 * Validation failed because multisig account tried to make transaction that is not allowed
	 */
	FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG(33),

	/**
	 * Validation failed because signer is already a cosigner of given multisig account.
	 */
	FAILURE_MULTISIG_ALREADY_A_COSIGNER(34),

	/**
	 * Validation failed because a multisig signature is attached to an incorrect multisig transaction.
	 */
	FAILURE_MULTISIG_MISMATCHED_SIGNATURE(35),

	/**
	 * Validation failed because a multisig modification contained multiple deletes.
	 */
	FAILURE_MULTISIG_MODIFICATION_MULTIPLE_DELETES(36),

	/**
	 * Validation failed because a multisig modification contained redundant modifications.
	 */
	FAILURE_MULTISIG_MODIFICATION_REDUNDANT_MODIFICATIONS(37),

	/**
	 * Validation failed because conflicting multisig modification is present.
	 */
	FAILURE_CONFLICTING_MULTISIG_MODIFICATION(38),

	/**
	 * Validation failed because a multisig modification would result in a multisig account having too many cosigners.
	 */
	FAILURE_TOO_MANY_MULTISIG_COSIGNERS(39),

	/**
	 * Validation failed because a transaction originated from the nemesis account after the nemesis block.
	 */
	FAILURE_NEMESIS_ACCOUNT_TRANSACTION_AFTER_NEMESIS_BLOCK(39);

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
