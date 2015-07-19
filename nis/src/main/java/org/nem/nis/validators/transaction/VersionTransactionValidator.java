package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.validators.*;

/**
 * A TransactionValidator implementation that applies to all transactions and validates that:
 * - higher versioned transactions do not appear before the respective fork heights
 */
public class VersionTransactionValidator implements SingleTransactionValidator {

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final int version = transaction.getEntityVersion();
		switch (transaction.getType()) {
			case TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION:
				switch (version) {
					case 1:
						return ValidationResult.SUCCESS;
					default:
						return context.getBlockHeight().getRaw() < BlockMarkerConstants.MULTISIG_M_OF_N_FORK(transaction.getVersion())
								? ValidationResult.FAILURE_MULTISIG_V2_AGGREGATE_MODIFICATION_BEFORE_FORK
								: ValidationResult.SUCCESS;
				}
		}

		return ValidationResult.SUCCESS;
	}
}