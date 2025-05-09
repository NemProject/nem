package org.nem.nis.validators.transaction;

import java.util.HashSet;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.validators.ValidationContext;

/**
 * Single transaction validator that validates a multisig aggregate modification:<br>
 * - Only adds accounts that are not already cosigners.<br>
 * - Only deletes accounts that are cosigners.<br>
 * - There are no duplicate add or delete modifications.<br>
 * - A delete aggregate modification can delete at most one account.<br>
 * - Only adds accounts that are not multisig accounts.
 */
public class MultisigCosignatoryModificationValidator implements TSingleTransactionValidator<MultisigAggregateModificationTransaction> {
	private final ReadOnlyAccountStateCache stateCache;
	private final BlockHeight multisigMofNForkHeight;

	/**
	 * Creates a new validator.
	 *
	 * @param stateCache The account state cache.
	 * @param multisigMofNForkHeight The multisig m-of-n fork height.
	 */
	public MultisigCosignatoryModificationValidator(final ReadOnlyAccountStateCache stateCache, final BlockHeight multisigMofNForkHeight) {
		this.stateCache = stateCache;
		this.multisigMofNForkHeight = multisigMofNForkHeight;
	}

	@Override
	public ValidationResult validate(final MultisigAggregateModificationTransaction transaction, final ValidationContext context) {
		if (this.multisigMofNForkHeight.getRaw() > context.getBlockHeight().getRaw() && transaction.getEntityVersion() != 1) {
			return ValidationResult.FAILURE_MULTISIG_V2_AGGREGATE_MODIFICATION_BEFORE_FORK;
		}

		final Address multisigAddress = transaction.getSigner().getAddress();

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

					if (isMultisig || cosignerAddress.equals(multisigAddress)) {
						return ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER;
					}

					accountsToAdd.add(cosignerAddress);
					break;

				case DelCosignatory:
					if (!isCosigner) {
						return ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER;
					}

					accountsToRemove.add(cosignerAddress);
					break;
				default :
					break;
			}
		}

		if (accountsToRemove.size() > 1) {
			return ValidationResult.FAILURE_MULTISIG_MODIFICATION_MULTIPLE_DELETES;
		}

		if (transaction.getCosignatoryModifications().size() != accountsToAdd.size() + accountsToRemove.size()) {
			return ValidationResult.FAILURE_MULTISIG_MODIFICATION_REDUNDANT_MODIFICATIONS;
		}

		final ReadOnlyAccountState multisigState = this.stateCache.findStateByAddress(multisigAddress);
		if (multisigState.getMultisigLinks().isCosignatory()) {
			return ValidationResult.FAILURE_MULTISIG_ACCOUNT_CANNOT_BE_COSIGNER;
		}

		return ValidationResult.SUCCESS;
	}
}
