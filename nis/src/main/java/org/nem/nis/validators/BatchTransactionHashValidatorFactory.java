package org.nem.nis.validators;

import org.nem.nis.dao.*;

/**
 * Factory for creating BatchTransactionHashValidator objects.
 */
public class BatchTransactionHashValidatorFactory {
	private final TransferDao transferDao;
	private final ImportanceTransferDao importanceTransferDao;

	/**
	 * Creates a new factory.
	 *
	 * @param transferDao The transfer dao.
	 * @param importanceTransferDao The importance transfer dao.
	 */
	public BatchTransactionHashValidatorFactory(
			final TransferDao transferDao,
			final ImportanceTransferDao importanceTransferDao) {
		this.transferDao = transferDao;
		this.importanceTransferDao = importanceTransferDao;
	}

	/**
	 * Creates a batch transaction hash validator.
	 *
	 * @return The validator.
	 */
	public BatchTransactionHashValidator create() {
		return new BatchTransactionHashValidator(this.transferDao, this.importanceTransferDao);
	}
}
