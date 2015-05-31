package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.validators.ValidationContext;

import java.util.HashSet;

/**
 * Single transaction validator that validates a multisig aggregate modification:
 * - Only adds accounts that are not already cosigners.
 * - Only deletes accounts that are cosigners.
 * - There are no duplicate add or delete modifications.
 * - A delete aggregate modification can delete at most one account.
 * - Only adds accounts that are not multisig accounts.
 */
public class MultisigAggregateModificationTransactionValidator implements TSingleTransactionValidator<MultisigAggregateModificationTransaction> {
	private final ReadOnlyAccountStateCache stateCache;

	/**
	 * Creates a new validator.
	 *
	 * @param stateCache The account state cache.
	 */
	public MultisigAggregateModificationTransactionValidator(final ReadOnlyAccountStateCache stateCache) {
		this.stateCache = stateCache;
	}

	@Override
	public ValidationResult validate(final MultisigAggregateModificationTransaction transaction, final ValidationContext context) {
		final Address multisigAddress = transaction.getSigner().getAddress();
		final ReadOnlyAccountState multisigState = this.stateCache.findStateByAddress(multisigAddress);
		int minCosignatories = multisigState.getMultisigLinks().minCosignatories();
		int numCosignatories = multisigState.getMultisigLinks().getCosignatories().size();

		final HashSet<Address> accountsToAdd = new HashSet<>();
		final HashSet<Address> accountsToRemove = new HashSet<>();
		for (final MultisigCosignatoryModification modification : transaction.getCosignatoryModifications()) {
			final Address cosignerAddress = modification.getCosignatory().getAddress();
			final ReadOnlyAccountState cosignerState = this.stateCache.findStateByAddress(cosignerAddress);
			final boolean isCosigner = cosignerState.getMultisigLinks().isCosignatoryOf(multisigAddress);
			final boolean isMultisig = cosignerState.getMultisigLinks().isMultisig();

			switch (modification.getModificationType()) {
				case AddCosignatory:
					if (isCosigner) {
						return ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER;
					}

					if (isMultisig) {
						return ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER;
					}

					accountsToAdd.add(cosignerAddress);
					numCosignatories++;
					break;

				case DelCosignatory:
					if (!isCosigner) {
						return ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER;
					}

					accountsToRemove.add(cosignerAddress);
					numCosignatories--;
					break;
			}
		}

		if (accountsToRemove.size() > 1) {
			return ValidationResult.FAILURE_MULTISIG_MODIFICATION_MULTIPLE_DELETES;
		}

		if (transaction.getCosignatoryModifications().size() != accountsToAdd.size() + accountsToRemove.size()) {
			return ValidationResult.FAILURE_MULTISIG_MODIFICATION_REDUNDANT_MODIFICATIONS;
		}

		if (multisigState.getMultisigLinks().isCosignatory()) {
			return ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER;
		}

		final MultisigMinCosignatoriesModification minCosignatoriesModification = transaction.getMinCosignatoriesModification();
		if (null != minCosignatoriesModification) {
			minCosignatories += minCosignatoriesModification.getRelativeChange();
		}

		// check if the final state is valid
		// minCosignatories equal to zero means all cosignatories have to sign
		if (0 > minCosignatories || minCosignatories > numCosignatories) {
			return ValidationResult.FAILURE_MULTISIG_MIN_COSIGNATORIES_OUT_OF_RANGE;
		}

		return ValidationResult.SUCCESS;
	}
}
