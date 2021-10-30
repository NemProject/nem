package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.validators.*;

/**
 * A TransactionValidator implementation that applies to all transactions and validates that higher versioned transactions do not appear
 * before the respective fork heights
 */
public class VersionTransactionValidator implements SingleTransactionValidator {

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final int version = transaction.getEntityVersion();
		final long blockHeight = context.getBlockHeight().getRaw();
		switch (transaction.getType()) {
			case TransactionTypes.PROVISION_NAMESPACE:
			case TransactionTypes.MOSAIC_DEFINITION_CREATION:
			case TransactionTypes.MOSAIC_SUPPLY_CHANGE:
				switch (version) {
					case 1:
						return blockHeight < BlockMarkerConstants.MOSAICS_FORK(transaction.getVersion())
								? ValidationResult.FAILURE_TRANSACTION_BEFORE_SECOND_FORK
								: ValidationResult.SUCCESS;
				}
				break;
			case TransactionTypes.TRANSFER:
				switch (version) {
					case 2:
						return blockHeight < BlockMarkerConstants.MOSAICS_FORK(transaction.getVersion())
								? ValidationResult.FAILURE_TRANSACTION_BEFORE_SECOND_FORK
								: ValidationResult.SUCCESS;
				}
				break;
			case TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION:
				switch (version) {
					case 2:
						return blockHeight < BlockMarkerConstants.MULTISIG_M_OF_N_FORK(transaction.getVersion())
								? ValidationResult.FAILURE_MULTISIG_V2_AGGREGATE_MODIFICATION_BEFORE_FORK
								: ValidationResult.SUCCESS;
				}
				break;
		}

		// TODO 20150811 J-*: add some extra logic to check height if existing mainnet / testnet fails validation
		return version == 1 ? ValidationResult.SUCCESS : ValidationResult.FAILURE_ENTITY_INVALID_VERSION;
	}
}
