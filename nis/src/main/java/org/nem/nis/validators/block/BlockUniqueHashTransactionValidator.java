package org.nem.nis.validators.block;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyHashCache;
import org.nem.nis.validators.BlockValidator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A block transaction validator that ensures all transaction hashes in the block are not known.
 */
public class BlockUniqueHashTransactionValidator implements BlockValidator {
	private final ReadOnlyHashCache transactionHashCache;

	/**
	 * Creates a new validator.
	 *
	 * @param transactionHashCache The cache of transaction hashes.
	 */
	public BlockUniqueHashTransactionValidator(final ReadOnlyHashCache transactionHashCache) {
		this.transactionHashCache = transactionHashCache;
	}

	@Override
	public ValidationResult validate(final Block block) {
		if (block.getTransactions().isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		final List<Hash> hashes = block.getTransactions().stream().map(HashUtils::calculateHash).collect(Collectors.toList());
		return this.transactionHashCache.anyHashExists(hashes) ? ValidationResult.NEUTRAL : ValidationResult.SUCCESS;
	}
}
