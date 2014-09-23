package org.nem.nis.validators;

import org.nem.nis.dao.TransferDao;
import org.nem.nis.poi.PoiFacade;

/**
 * Factory for creating TransactionValidator objects.
 */
public class TransactionValidatorFactory {
	private final TransferDao transferDao;

	/**
	 * Creates a new factory.
	 *
	 * @param transferDao The transfer dao.
	 */
	public TransactionValidatorFactory(final TransferDao transferDao) {
		this.transferDao = transferDao;
	}

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
		builder.add(new UniqueHashTransactionValidator(this.transferDao));
		return builder.build();
	}
}
