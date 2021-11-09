package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyMultisigLinks;
import org.nem.nis.validators.ValidationContext;

/**
 * Single transaction validator that validates a multisig aggregate modification:<br>
 * - will not cause a multisig account to have greater than MAX_ALLOWED_COSIGNATORIES_PER_ACCOUNT cosigners<br>
 * - will not cause the min cosignatories value to be out of range (less than zero or greater than the number of cosigners)
 */
public class NumCosignatoryRangeValidator implements TSingleTransactionValidator<MultisigAggregateModificationTransaction> {
	private final ReadOnlyAccountStateCache stateCache;

	/**
	 * Creates a new validator.
	 *
	 * @param stateCache The account state cache.
	 */
	public NumCosignatoryRangeValidator(final ReadOnlyAccountStateCache stateCache) {
		this.stateCache = stateCache;
	}

	@Override
	public ValidationResult validate(final MultisigAggregateModificationTransaction transaction, final ValidationContext context) {
		final Address multisigAddress = transaction.getSigner().getAddress();
		final ReadOnlyMultisigLinks multisigLinks = this.stateCache.findStateByAddress(multisigAddress).getMultisigLinks();
		int numCosigners = multisigLinks.getCosignatories().size();

		for (final MultisigCosignatoryModification modification : transaction.getCosignatoryModifications()) {
			switch (modification.getModificationType()) {
				case AddCosignatory:
					++numCosigners;
					break;

				case DelCosignatory:
					--numCosigners;
					break;
				default :
					break;
			}
		}

		int minCosigners = multisigLinks.minCosignatories();
		final MultisigMinCosignatoriesModification minCosignatoriesModification = transaction.getMinCosignatoriesModification();
		if (null != minCosignatoriesModification) {
			minCosigners += minCosignatoriesModification.getRelativeChange();
		}

		// minCosigners equal to zero means all cosignatories have to sign
		if (minCosigners < 0 || minCosigners > numCosigners) {
			return ValidationResult.FAILURE_MULTISIG_MIN_COSIGNATORIES_OUT_OF_RANGE;
		}

		return numCosigners > BlockChainConstants.MAX_ALLOWED_COSIGNATORIES_PER_ACCOUNT
				? ValidationResult.FAILURE_TOO_MANY_MULTISIG_COSIGNERS
				: ValidationResult.SUCCESS;
	}
}
