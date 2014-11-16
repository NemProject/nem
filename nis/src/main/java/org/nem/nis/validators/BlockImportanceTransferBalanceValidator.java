package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;
import java.util.stream.*;

// TODO: having two separate validators is definitely nicer and easier to test, but it might
// TODO: have more sense to join them into one

/**
 * A batch transaction validator, that verifies that inside block, there are no transfers to accounts,
 * that are used as remote harvesting accounts.
 */
public class BlockImportanceTransferBalanceValidator implements BlockValidator {
	@Override
	public ValidationResult validate(final Block block) {
		if (block.getTransactions().isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		final Set<Address> importanceTransfers = block.getTransactions().stream()
				.filter(t -> t.getType() == TransactionTypes.IMPORTANCE_TRANSFER)
				.map(t -> ((ImportanceTransferTransaction)t).getRemote().getAddress())
				.collect(Collectors.toSet());
		if (importanceTransfers.isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		// note: it might be transfer with amount of 0, but I guess we don't have to care about it
		final boolean hasTransfer = block.getTransactions().stream()
				.filter(t -> t.getType() == TransactionTypes.TRANSFER)
				.anyMatch(
						t -> importanceTransfers.contains(((TransferTransaction)t).getRecipient().getAddress())
				);
		return hasTransfer ? ValidationResult.FAILURE_CONFLICTING_IMPORTANCE_TRANSFER : ValidationResult.SUCCESS;
	}
}
