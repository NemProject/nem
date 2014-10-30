package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dao.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * A batch transaction validator that ensures all transaction hashes are unique.
 */
public class BatchTransactionHashValidator {
	private static final Logger LOGGER = Logger.getLogger(BatchTransactionHashValidator.class.getName());
	private final TransferDao transferDao;
	private final ImportanceTransferDao importanceTransferDao;

	/**
	 * Creates a new validator.
	 *
	 * @param transferDao The transfer dao.
	 */
	public BatchTransactionHashValidator(
			final TransferDao transferDao,
			final ImportanceTransferDao importanceTransferDao) {
		this.transferDao = transferDao;
		this.importanceTransferDao = importanceTransferDao;
	}

	public ValidationResult validate(final Collection<Transaction> transactions, final ValidationContext context) {
		long start = System.currentTimeMillis();
		final Collection<Hash> hashes = new ArrayList<>();
		transactions.stream().forEach(t -> hashes.add(HashUtils.calculateHash(t)));
		final BlockHeight blockHeight = context.getConfirmedBlockHeight();
		final boolean isInTransferDao = this.transferDao.duplicateHashExists(hashes, blockHeight);
		final boolean isInImportanceTransferDao = this.importanceTransferDao.duplicateHashExists(hashes, blockHeight);
		long stop = System.currentTimeMillis();
		LOGGER.info(String.format("BatchTransactionHashValidator.validate() at height %d needed %dms.",
				blockHeight.getRaw(),
				stop - start));
		return isInTransferDao || isInImportanceTransferDao
				? ValidationResult.NEUTRAL
				: ValidationResult.SUCCESS;
	}
}
