package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.validators.*;

/**
 * Adapter that adapts a strongly typed transaction validator to a single transaction validator.
 *
 * @param <TTransaction> The supported transaction type.
 */
public class TSingleTransactionValidatorAdapter<TTransaction extends Transaction> implements SingleTransactionValidator {
	private final int transactionType;
	private final TSingleTransactionValidator<TTransaction> innerValidator;

	/**
	 * Creates a new validator.
	 *
	 * @param transactionType The supported transaction type.
	 * @param innerValidator The inner validator.
	 */
	public TSingleTransactionValidatorAdapter(final int transactionType, final TSingleTransactionValidator<TTransaction> innerValidator) {
		this.transactionType = transactionType;
		this.innerValidator = innerValidator;
	}

	@Override
	public String getName() {
		return this.innerValidator.getName();
	}

	@Override
	@SuppressWarnings("unchecked")
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		return this.transactionType != transaction.getType()
				? ValidationResult.SUCCESS
				: this.innerValidator.validate((TTransaction) transaction, context);
	}
}
