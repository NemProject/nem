package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

public class MultisigSignerModificationTransactionValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;

	public MultisigSignerModificationTransactionValidator(final ReadOnlyAccountStateCache stateCache) {
		this.stateCache = stateCache;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		if (context.getBlockHeight().getRaw() < BlockMarkerConstants.BETA_MULTISIG_FORK) {
			return ValidationResult.FAILURE_ENTITY_UNUSABLE;
		}

		return this.validate((MultisigAggregateModificationTransaction)transaction, context);
	}

	private ValidationResult validate(final MultisigAggregateModificationTransaction transaction, final ValidationContext context) {
		return ValidationResult.aggregate(transaction.getModifications().stream()
				.map(t -> this.validate(transaction.getSigner().getAddress(), t)).iterator());
	}

	private ValidationResult validate(final Address multisigAddress, final MultisigModification modification) {
		final ReadOnlyAccountState cosignerState = this.stateCache.findStateByAddress(modification.getCosignatory().getAddress());

		if (MultisigModificationType.Add == modification.getModificationType() && cosignerState.getMultisigLinks().isCosignatoryOf(multisigAddress)) {
			return ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER;
		}

		if (MultisigModificationType.Del == modification.getModificationType() && ! cosignerState.getMultisigLinks().isCosignatoryOf(multisigAddress)) {
			return ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER;
		}
		return ValidationResult.SUCCESS;
	}
}
