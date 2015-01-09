package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.Set;
import java.util.stream.Collectors;

// TODO 2014116 J-G: i don't think you're covering this scenario (or else i'm not sure how it is being covered):
// > block N sets up R as a remote account
// > block N + 1 bob makes a transfer to R
// > since it is not the same block this validator will succeed.
// TODO 20141116 G-J: yes it doesn't, if we need to cover it it should be covered by
// some (transfer) transaction validator... (check if recipient has associated "owner")
// but I'm not sure if we actually need to cover such scenario.

/**
 * A block transaction validator, that verifies that inside a block, there are no transfers to accounts
 * that are used as remote harvesting accounts.
 */
public class BlockImportanceTransferBalanceValidator implements BlockValidator {
	@Override
	public ValidationResult validate(final Block block) {
		final Set<Address> importanceTransfers = block.getTransactions().stream()
				.filter(t -> t.getType() == TransactionTypes.IMPORTANCE_TRANSFER)
				.map(t -> ((ImportanceTransferTransaction)t).getRemote().getAddress())
				.collect(Collectors.toSet());

		// most blocks don't contain importance transfer, so it has sense to do short circuit
		if (importanceTransfers.isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		// note: it might be transfer with amount of 0, but I guess we don't have to care about it
		final boolean hasTransfer = block.getTransactions().stream()
				.filter(t -> t.getType() == TransactionTypes.TRANSFER)
				.anyMatch(t -> importanceTransfers.contains(((TransferTransaction)t).getRecipient().getAddress()));
		return hasTransfer ? ValidationResult.FAILURE_DESTINATION_ACCOUNT_HAS_NONZERO_BALANCE : ValidationResult.SUCCESS;
	}
}
