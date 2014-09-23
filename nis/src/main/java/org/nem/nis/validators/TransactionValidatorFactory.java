package org.nem.nis.validators;

import org.nem.core.time.TimeProvider;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.poi.PoiFacade;

/**
 * Factory for creating TransactionValidator objects.
 */
public class TransactionValidatorFactory {
	private final TransferDao transferDao;
	private final TimeProvider timeProvider;

	/**
	 * Creates a new factory.
	 *
	 * @param transferDao The transfer dao.
	 * @param timeProvider The time provider.
	 */
	public TransactionValidatorFactory(
			final TransferDao transferDao,
			final TimeProvider timeProvider) {
		this.transferDao = transferDao;
		this.timeProvider = timeProvider;
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
		builder.add(new NonFutureEntityValidator(this.timeProvider));
		builder.add(new TransferTransactionValidator());
		builder.add(new ImportanceTransferTransactionValidator(poiFacade));
		builder.add(new UniqueHashTransactionValidator(this.transferDao));
		return builder.build();
	}
}
