package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A batch transaction validator that ensures all importance transactions are non-conflicting.
 */
public class BatchImportanceTransferValidator implements BatchTransactionValidator {

	// TODO 20141116 J-G: obviously we need tests :)

	@Override
	public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions) {
		for (final TransactionsContextPair pair : groupedTransactions) {
			final List<Transaction> transactions = pair.getTransactions().stream()
					.filter(t -> t.getType() == TransactionTypes.IMPORTANCE_TRANSFER)
					.collect(Collectors.toList());

			final SingleTransactionValidator validator = new NonConflictingImportanceTransferTransactionValidator(() -> transactions);
			final ValidationResult result = ValidationResult.aggregate(transactions.stream().map(t -> validator.validate(t)).iterator());
			if (result.isFailure()) {
				return result;
			}
		}

		return ValidationResult.SUCCESS;
	}
}