package org.nem.nis.validators;

import org.nem.nis.poi.PoiFacade;

/**
 * Factory for creating TransactionValidator objects.
 */
public class TransactionValidatorFactory {

	/**
	 * Creates a transaction validator.
	 *
	 * @param poiFacade The poi facade.
	 * @return The validator.
	 */
	public TransactionValidator create(final PoiFacade poiFacade) {
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();
		builder.add(new UniversalTransactionValidator());
		builder.add(new TransferTransactionValidator());
		builder.add(new ImportanceTransferTransactionValidator(poiFacade));
		return builder.build();
	}
}
