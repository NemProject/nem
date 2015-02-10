package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.validators.*;

import java.util.HashSet;

/**
 * Single transaction validator that validates a multisig aggregate modification:
 * - Only adds accounts that are not already cosigners.
 * - Only deletes accounts that are cosigners.
 * - There are no duplicate add or delete modifications.
 * - A delete aggregate modification can delete at most one account.
 */
public class MultisigAggregateModificationTransactionValidator implements SingleTransactionValidator {
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
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		return this.validate((MultisigAggregateModificationTransaction)transaction);
	}

	private ValidationResult validate(final MultisigAggregateModificationTransaction transaction) {
		final Address multisigAddress = transaction.getSigner().getAddress();

		final HashSet<Address> accountsToAdd = new HashSet<>();
		final HashSet<Address> accountsToRemove = new HashSet<>();
		for (final MultisigModification modification : transaction.getModifications()) {
			final Address cosignerAddress = modification.getCosignatory().getAddress();
			final ReadOnlyAccountState cosignerState = this.stateCache.findStateByAddress(cosignerAddress);
			final boolean isCosigner = cosignerState.getMultisigLinks().isCosignatoryOf(multisigAddress);

			switch (modification.getModificationType()) {
				case Add:
					if (isCosigner) {
						return ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER;
					}

					accountsToAdd.add(cosignerAddress);
					break;

				case Del:
					if (!isCosigner) {
						return ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER;
					}

					// TODO 20150122 J-G: now this is being checked in two validators (this one and MultisigSignaturesPresentValidator)
					if (!accountsToRemove.isEmpty()) {
						return ValidationResult.FAILURE_MULTISIG_MODIFICATION_MULTIPLE_DELETES;
					}

					accountsToRemove.add(cosignerAddress);
					break;
			}
		}

		if (transaction.getModifications().size() != accountsToAdd.size() + accountsToRemove.size()) {
			return ValidationResult.FAILURE_MULTISIG_MODIFICATION_REDUNDANT_MODIFICATIONS;
		}

		return ValidationResult.SUCCESS;
	}
}
