package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A batch transaction validator that ensures all transaction hashes are unique.
 */
public class BatchUniqueHashTransactionValidator implements BatchTransactionValidator {
	private static final Logger LOGGER = Logger.getLogger(BatchUniqueHashTransactionValidator.class.getName());

	private final HashCache transactionHashCache;

	/**
	 * Creates a new validator.
	 *
	 * @param transactionHashCache The transaction hash cache.
	 */
	public BatchUniqueHashTransactionValidator(
			final HashCache transactionHashCache) {
		this.transactionHashCache = transactionHashCache;
	}

	@Override
	public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions) {
		if (groupedTransactions.isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		final BlockHeight blockHeight = groupedTransactions.get(0).getContext().getConfirmedBlockHeight();
		final List<Hash> hashes = groupedTransactions.stream()
				.flatMap(pair -> pair.getTransactions().stream())
				.map(HashUtils::calculateHash)
				.collect(Collectors.toList());

		final long start = System.currentTimeMillis();
		final ValidationResult result = this.validate(hashes);
		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("BatchTransactionHashValidator.validate() at height %d needed %dms.",
				blockHeight.getRaw(),
				stop - start));
		return result;
	}

	private ValidationResult validate(final Collection<Hash> hashes) {
		return this.transactionHashCache.anyHashExists(hashes)
				? ValidationResult.NEUTRAL
				: ValidationResult.SUCCESS;
	}
}