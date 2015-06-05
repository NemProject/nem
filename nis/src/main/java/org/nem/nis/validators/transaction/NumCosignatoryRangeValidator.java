package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.validators.ValidationContext;

/**
 * Single transaction validator that validates a multisig aggregate modification:
 * - will not cause a multisig account to have greater than MAX_ALLOWED_COSIGNATORIES_PER_ACCOUNT cosigners
 * - will not cause the min cosignatories value to be out of range
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
		int numCosigners = this.stateCache.findStateByAddress(multisigAddress).getMultisigLinks().getCosignatories().size();

		for (final MultisigCosignatoryModification modification : transaction.getCosignatoryModifications()) {
			switch (modification.getModificationType()) {
				case AddCosignatory:
					++numCosigners;
					break;

				case DelCosignatory:
					--numCosigners;
					break;
			}
		}

//		final ReadOnlyAccountState multisigState = this.stateCache.findStateByAddress(multisigAddress);
//		int minCosignatories = multisigState.getMultisigLinks().minCosignatories();

		//		// TODO 20150531 J-B: any reason you didn't want a separate min cosignatories validator?
//		// TODO 20150601 BR -> J: while that is possible it would mean the min cosignatories validator has to go through the
//		// > cosignatory modifications again to calculate the resulting number of cosignatories. If you want to have separate
//		// > validators we have to rename this one too.
//		final MultisigMinCosignatoriesModification minCosignatoriesModification = transaction.getMinCosignatoriesModification();
//		if (null != minCosignatoriesModification) {
//			minCosignatories += minCosignatoriesModification.getRelativeChange();
//		}
//
//		// check if the final state is valid
//		// minCosignatories equal to zero means all cosignatories have to sign
//		if (0 > minCosignatories || minCosignatories > numCosignatories) {
//			return ValidationResult.FAILURE_MULTISIG_MIN_COSIGNATORIES_OUT_OF_RANGE;
//		}

		return numCosigners > BlockChainConstants.MAX_ALLOWED_COSIGNATORIES_PER_ACCOUNT
				? ValidationResult.FAILURE_TOO_MANY_MULTISIG_COSIGNERS
				: ValidationResult.SUCCESS;
	}
}