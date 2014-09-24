package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.dao.*;

/**
 * A transaction validator that ensures transaction hashes are unique.
 */
public class UniqueHashTransactionValidator implements TransactionValidator {
	private final TransferDao transferDao;
	private final ImportanceTransferDao importanceTransferDao;

	/**
	 * Creates a new validator.
	 *
	 * @param transferDao The transfer dao.
	 */
	public UniqueHashTransactionValidator(
			final TransferDao transferDao,
			final ImportanceTransferDao importanceTransferDao) {
		this.transferDao = transferDao;
		this.importanceTransferDao = importanceTransferDao;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (context.getBlockHeight().getRaw() < BlockMarkerConstants.FATAL_TX_BUG_HEIGHT) {
			return ValidationResult.SUCCESS;
		}

		final Hash hash = HashUtils.calculateHash(transaction);
		return null != this.transferDao.findByHash(hash.getRaw()) || null != this.importanceTransferDao.findByHash(hash.getRaw())
				? ValidationResult.NEUTRAL
				: ValidationResult.SUCCESS;
	}
}