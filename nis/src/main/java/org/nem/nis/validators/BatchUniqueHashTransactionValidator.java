package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dao.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A batch transaction validator that ensures all transaction hashes are unique.
 */
public class BatchUniqueHashTransactionValidator implements BatchTransactionValidator {
	private static final Logger LOGGER = Logger.getLogger(BatchUniqueHashTransactionValidator.class.getName());

	private final TransferDao transferDao;
	private final ImportanceTransferDao importanceTransferDao;

	/**
	 * Creates a new validator.
	 *
	 * @param transferDao The transfer dao.
	 */
	public BatchUniqueHashTransactionValidator(
			final TransferDao transferDao,
			final ImportanceTransferDao importanceTransferDao) {
		this.transferDao = transferDao;
		this.importanceTransferDao = importanceTransferDao;
	}

	@Override
	public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions) {
		if (groupedTransactions.isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		final BlockHeight blockHeight = groupedTransactions.get(0).getContext().getConfirmedBlockHeight();
		final List<Hash> hashes = new ArrayList<>();
		for (final TransactionsContextPair pair : groupedTransactions) {
			hashes.addAll(pair.getTransactions().stream().map(HashUtils::calculateHash).collect(Collectors.toList()));
		}

		final long start = System.currentTimeMillis();
		final ValidationResult result = this.validate(hashes, blockHeight);
		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("BatchTransactionHashValidator.validate() at height %d needed %dms.",
				blockHeight.getRaw(),
				stop - start));
		return result;
	}

	private ValidationResult validate(final Collection<Hash> hashes, final BlockHeight blockHeight) {
		final boolean isInTransferDao = this.transferDao.anyHashExists(hashes, blockHeight);
		final boolean isInImportanceTransferDao = this.importanceTransferDao.anyHashExists(hashes, blockHeight);
		return isInTransferDao || isInImportanceTransferDao
				? ValidationResult.NEUTRAL
				: ValidationResult.SUCCESS;
	}
}