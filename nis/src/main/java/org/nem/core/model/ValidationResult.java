package org.nem.core.model;

/**
 * Possible validation results.
 */
public enum ValidationResult {

	/**
	 * Validation succeeded.
	 */
	SUCCESS,

	/**
	 * Validation failed because the deadline passed.
	 */
	FAILURE_PAST_DEADLINE,

	/**
	 * Validation failed because the deadline is too far in the future.
	 */
	FAILURE_FUTURE_DEADLINE,

	/**
	 * Validation failed because the account had an insufficient balance.
	 */
	FAILURE_INSUFFICIENT_BALANCE,

	/**
	 * Validation failed because the message is too large.
	 */
	FAILURE_MESSAGE_TOO_LARGE,

	/**
	 * Validation failed for an unknown reason.
	 */
	FAILURE_UNKNOWN
}
