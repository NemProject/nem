package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.dao.TransferDao;

/**
 * A transaction validator that ensures transaction hashes are unique.
 */
public class UniqueHashTransactionValidator implements TransactionValidator {
	private final TransferDao transferDao;

	/**
	 * Creates a new validator.
	 *
	 * @param transferDao The transfer dao.
	 */
	public UniqueHashTransactionValidator(final TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	// TODO-CR 20140923 G-J: I think this should check both TransferDao and ImportanceTransferDao, shouldn't it?
	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (context.getBlockHeight().getRaw() < BlockMarkerConstants.FATAL_TX_BUG_HEIGHT) {
			return ValidationResult.SUCCESS;
		}

		final Hash hash = HashUtils.calculateHash(transaction);
		return null != this.transferDao.findByHash(hash.getRaw())
				? ValidationResult.NEUTRAL
				: ValidationResult.SUCCESS;
	}
}