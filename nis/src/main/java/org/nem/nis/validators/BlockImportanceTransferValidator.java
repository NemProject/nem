package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.poi.PoiFacade;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A block transaction validator that ensures all importance transactions within a block are non-conflicting.
 */
public class BlockImportanceTransferValidator implements BlockValidator {
	private final PoiFacade poiFacade;

	/**
	 * Creates an observer.
	 *
	 * @param poiFacade The poi facade.
	 */
	public BlockImportanceTransferValidator(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
	}

	@Override
	public ValidationResult validate(final Block block) {
		if (block.getHeight().getRaw() < BlockMarkerConstants.BETA_IT_VALIDATION_FORK) {
			return ValidationResult.SUCCESS;
		}

		final List<Transaction> importanceTransfers = block.getTransactions().stream()
				.filter(t -> t.getType() == TransactionTypes.IMPORTANCE_TRANSFER)
				.collect(Collectors.toList());

		// most blocks don't contain importance transfer, so it has sense to do short circuit
		if (importanceTransfers.isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		final ValidationContext validationContext = new ValidationContext(this.poiFacade.getDebitPredicate());
		final SingleTransactionValidator validator = new NonConflictingImportanceTransferTransactionValidator(() -> importanceTransfers);
		return ValidationResult.aggregate(importanceTransfers.stream().map(t -> validator.validate(t, validationContext)).iterator());
	}
}