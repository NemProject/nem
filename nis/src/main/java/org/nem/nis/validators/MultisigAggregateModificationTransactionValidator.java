package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

import java.util.HashSet;
import java.util.Set;

public class MultisigAggregateModificationTransactionValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;
	private final Set<Address> multisigModificationSenders = new HashSet<>();

	public MultisigAggregateModificationTransactionValidator(final ReadOnlyAccountStateCache stateCache) {
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
		// TODO 20140106 J-G: should add test for this
		/**
		 * We don't want to allow multiple AggregateModificationTransaction from single account, this handles it.
		 * This validator is also used in UnconfirmedTransactions, so multiple AggregateModificationTransaction,
		 * won't be added to UnconfirmedTransactions.
		 */
		if (! this.multisigModificationSenders.add(transaction.getSigner().getAddress())) {
			return ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG;
		}

		return ValidationResult.aggregate(transaction.getModifications().stream()
				.map(t -> this.validate(transaction.getSigner().getAddress(), t)).iterator());
	}

	private ValidationResult validate(final Address multisigAddress, final MultisigModification modification) {
		final ReadOnlyAccountState cosignerState = this.stateCache.findStateByAddress(modification.getCosignatory().getAddress());

		if (MultisigModificationType.Add == modification.getModificationType() && cosignerState.getMultisigLinks().isCosignatoryOf(multisigAddress)) {
			return ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER;
		}

		if (MultisigModificationType.Del == modification.getModificationType() && !cosignerState.getMultisigLinks().isCosignatoryOf(multisigAddress)) {
			return ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER;
		}
		return ValidationResult.SUCCESS;
	}
}
