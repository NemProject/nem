package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
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

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final Hash hash = HashUtils.calculateHash(transaction);
		return null != this.transferDao.findByHash(hash.getRaw())
				? ValidationResult.NEUTRAL
				: ValidationResult.SUCCESS;
	}
}