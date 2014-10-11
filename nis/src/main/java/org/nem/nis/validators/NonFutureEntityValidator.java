package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.time.*;

/**
 * BlockValidator and TransactionValidator implementation that ensures entities are not too far in the future.
 */
public class NonFutureEntityValidator implements BlockValidator, TransactionValidator {
	private static final int MAX_ALLOWED_SECONDS_AHEAD_OF_TIME = 60;
	private final TimeProvider timeProvider;

	/**
	 * Creates a new validator.
	 *
	 * @param timeProvider The time provider.
	 */
	public NonFutureEntityValidator(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	@Override
	public ValidationResult validate(final Block block) {
		return this.validateTimeStamp(block.getTimeStamp());
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		return this.validateTimeStamp(transaction.getTimeStamp());
	}

	private ValidationResult validateTimeStamp(final TimeInstant timeInstant) {
		final TimeInstant currentTime = this.timeProvider.getCurrentTime();
		return timeInstant.compareTo(currentTime.addSeconds(MAX_ALLOWED_SECONDS_AHEAD_OF_TIME)) > 0
				? ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE
				: ValidationResult.SUCCESS;
	}
}
