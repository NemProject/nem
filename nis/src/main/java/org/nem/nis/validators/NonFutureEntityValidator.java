package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.time.*;

/**
 * Base class for validators that validates entities are not too far in the future
 */
public abstract class NonFutureEntityValidator {
	private final TimeProvider timeProvider;

	/**
	 * Creates a new validator.
	 *
	 * @param timeProvider The time provider.
	 */
	protected NonFutureEntityValidator(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	/**
	 * Validates the specified time stamp.
	 *
	 * @param timeStamp The time stamp.
	 * @return The validation result.
	 */
	protected ValidationResult validateTimeStamp(final TimeInstant timeStamp) {
		final TimeInstant currentTime = this.timeProvider.getCurrentTime();
		return timeStamp.compareTo(currentTime.addSeconds(BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME)) > 0
				? ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE
				: ValidationResult.SUCCESS;
	}
}
