package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.time.TimeProvider;

/**
 * Single transaction validator that validates:
 * - the transaction time stamp is not too far in the future
 */
public class TransactionNonFutureEntityValidator extends NonFutureEntityValidator implements SingleTransactionValidator {

	/**
	 * Creates a new validator.
	 *
	 * @param timeProvider The time provider.
	 */
	public TransactionNonFutureEntityValidator(final TimeProvider timeProvider) {
		super(timeProvider);
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		return this.validateTimeStamp(transaction.getTimeStamp());
	}
}
