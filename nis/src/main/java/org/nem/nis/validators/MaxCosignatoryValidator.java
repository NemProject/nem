package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.cache.ReadOnlyAccountStateCache;

/**
 * Single transaction validator that validates a multisig aggregate modification:
 * - will not cause a multisig account to have greater than MAX_ALLOWED_COSIGNATORIES_PER_ACCOUNT cosigners
 */
public class MaxCosignatoryValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;

	/**
	 * Creates a new validator.
	 *
	 * @param stateCache The account state cache.
	 */
	public MaxCosignatoryValidator(final ReadOnlyAccountStateCache stateCache) {
		this.stateCache = stateCache;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		return this.validate((MultisigAggregateModificationTransaction)transaction);
	}

	private ValidationResult validate(final MultisigAggregateModificationTransaction transaction) {
		final Address multisigAddress = transaction.getSigner().getAddress();
		int numCosigners = this.stateCache.findStateByAddress(multisigAddress).getMultisigLinks().getCosignatories().size();

		for (final MultisigModification modification : transaction.getModifications()) {
			switch (modification.getModificationType()) {
				case Add:
					++numCosigners;
					break;

				case Del:
					--numCosigners;
					break;
			}
		}

		return numCosigners > BlockChainConstants.MAX_ALLOWED_COSIGNATORIES_PER_ACCOUNT
				? ValidationResult.FAILURE_TOO_MANY_MULTISIG_COSIGNERS
				: ValidationResult.SUCCESS;
	}
}