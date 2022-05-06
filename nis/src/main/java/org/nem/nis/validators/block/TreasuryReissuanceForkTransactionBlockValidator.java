package org.nem.nis.validators.block;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.nis.validators.BlockValidator;
import org.nem.nis.ForkConfiguration;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Validator that checks that all treasury reissuance transactions are present at fork height.
 */
public class TreasuryReissuanceForkTransactionBlockValidator implements BlockValidator {
	private static final Logger LOGGER = Logger.getLogger(TreasuryReissuanceForkTransactionBlockValidator.class.getName());

	private final ForkConfiguration forkConfiguration;

	/**
	 * Creates a validator.
	 *
	 * @param forkConfiguration The fork configuration.
	 */
	public TreasuryReissuanceForkTransactionBlockValidator(final ForkConfiguration forkConfiguration) {
		this.forkConfiguration = forkConfiguration;
	}

	@Override
	public ValidationResult validate(final Block block) {
		if (!this.forkConfiguration.getTreasuryReissuanceForkHeight().equals(block.getHeight())) {
			return ValidationResult.SUCCESS;
		}

		final List<Hash> blockTransactionHashes = block.getTransactions().stream().map(tx -> HashUtils.calculateHash(tx))
				.collect(Collectors.toList());

		if (this.forkConfiguration.getTreasuryReissuanceForkTransactionHashes().equals(blockTransactionHashes)) {
			LOGGER.info(String.format("Fork block at %s had expected preferred transactions", block.getHeight()));
			return ValidationResult.SUCCESS;
		}

		if (this.forkConfiguration.getTreasuryReissuanceForkFallbackTransactionHashes().equals(blockTransactionHashes)) {
			LOGGER.info(String.format("Fork block at %s had expected fallback transactions", block.getHeight()));
			return ValidationResult.SUCCESS;
		}

		LOGGER.warning(String.format("Fork block at %s did not have expected transactions, it had (%d)\n%s", block.getHeight(),
				blockTransactionHashes.size(),
				String.join("\n", blockTransactionHashes.stream().map(Hash::toString).collect(Collectors.toList()))));
		return ValidationResult.FAILURE_UNKNOWN;
	}
}
