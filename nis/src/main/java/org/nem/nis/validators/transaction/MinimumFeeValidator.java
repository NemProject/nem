package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.validators.*;

/**
 * A TransactionValidator implementation that applies to all transactions and validates that:
 * - the transaction fee is at least as large as the minimum fee
 */
public class MinimumFeeValidator implements SingleTransactionValidator {

	/**
	 * Creates a validator.
	 */
	public MinimumFeeValidator() {
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final TransactionFeeCalculator calculator = NemGlobals.getTransactionFeeCalculator();
		return calculator.isFeeValid(transaction, context.getBlockHeight())
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_INSUFFICIENT_FEE;
	}
}
