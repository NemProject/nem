package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.validators.ValidationContext;

/**
 * Interface for validating a strongly typed single transaction.
 *
 * @param <TTransaction> The supported transaction type.
 */
public interface TSingleTransactionValidator<TTransaction extends Transaction> {

	/**
	 * Gets the name of the validator.
	 *
	 * @return The name of the validator.
	 */
	default String getName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Checks the validity of the specified transaction.
	 *
	 * @param transaction The transaction.
	 * @param context The validation context.
	 * @return The validation result.
	 */
	ValidationResult validate(final TTransaction transaction, final ValidationContext context);
}
