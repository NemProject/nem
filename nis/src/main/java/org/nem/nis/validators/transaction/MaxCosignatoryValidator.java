package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.validators.ValidationContext;

/**
 * Single transaction validator that validates a multisig aggregate modification:
 * - will not cause a multisig account to have greater than MAX_ALLOWED_COSIGNATORIES_PER_ACCOUNT cosigners
 */
public class MaxCosignatoryValidator implements TSingleTransactionValidator<MultisigAggregateModificationTransaction> {
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
	public ValidationResult validate(final MultisigAggregateModificationTransaction transaction, final ValidationContext context) {
		final Address multisigAddress = transaction.getSigner().getAddress();
		int numCosigners = this.stateCache.findStateByAddress(multisigAddress).getMultisigLinks().getCosignatories().size();

		for (final MultisigModification modification : transaction.getModifications()) {
			switch (modification.getModificationType()) {
				case AddCosignatory:
					++numCosigners;
					break;

				case DelCosignatory:
					--numCosigners;
					break;
			}
		}

		return numCosigners > BlockChainConstants.MAX_ALLOWED_COSIGNATORIES_PER_ACCOUNT
				? ValidationResult.FAILURE_TOO_MANY_MULTISIG_COSIGNERS
				: ValidationResult.SUCCESS;
	}
}