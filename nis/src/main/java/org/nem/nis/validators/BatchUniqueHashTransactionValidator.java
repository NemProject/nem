package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A batch transaction validator that ensures all transaction hashes are unique.
 */
public class BatchUniqueHashTransactionValidator implements BatchTransactionValidator {
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

		final List<Hash> hashes = groupedTransactions.stream()
				.flatMap(pair -> pair.getTransactions().stream())
				.map(HashUtils::calculateHash)
				.collect(Collectors.toList());

		final ValidationResult result = this.validate(hashes);
		return result;
	}

	private ValidationResult validate(final Collection<Hash> hashes) {
		return this.transactionHashCache.anyHashExists(hashes)
				? ValidationResult.NEUTRAL
				: ValidationResult.SUCCESS;
	}
}