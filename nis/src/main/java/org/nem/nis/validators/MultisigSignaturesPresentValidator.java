package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

import java.util.List;
import java.util.stream.Collectors;

// NOTE: this validator is used only during block creation, or when receiving block
public class MultisigSignaturesPresentValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;

	public MultisigSignaturesPresentValidator(final ReadOnlyAccountStateCache stateCache) {
		this.stateCache = stateCache;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.MULTISIG != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		if (context.getBlockHeight().getRaw() < BlockMarkerConstants.BETA_MULTISIG_FORK) {
			return ValidationResult.FAILURE_ENTITY_UNUSABLE;
		}

		return this.validate((MultisigTransaction)transaction, context);
	}

	private ValidationResult validate(final MultisigTransaction transaction, final ValidationContext context) {
		final ReadOnlyAccountState multisigAddress = this.stateCache.findStateByAddress(transaction.getOtherTransaction().getSigner().getAddress());
		// TODO 20150103 J-G: why are you special casing this (unlikely) case?
		if (multisigAddress.getMultisigLinks().getCosignatories().size() == 1) {
			return ValidationResult.SUCCESS;
		}

		// TODO 20150103 J-G: do you have a validator that prevents all cosigner accounts from being removed?

		// TODO 20150103 J-G: consider refactoring this block into a separate function
		Address accountForRemoval = null;
		if (transaction.getOtherTransaction().getType() == TransactionTypes.MULTISIG_SIGNER_MODIFY) {
			final MultisigSignerModificationTransaction modificationTransaction = (MultisigSignerModificationTransaction)transaction.getOtherTransaction();
			final List<Address> removal = modificationTransaction.getModifications().stream()
					.filter(m -> m.getModificationType() == MultisigModificationType.Del)
					.map(m -> m.getCosignatory().getAddress())
					.collect(Collectors.toList());
			if (removal.size() == 1) {
				accountForRemoval = removal.get(0);
			}
		}

		final Hash transactionHash = transaction.getOtherTransactionHash();
		// this loop could be done using reduce, but I'm leaving it like this for readability
		// TODO: this probably should be done differently, as right now it allows more MultisigSignatures, than there are actual cosigners
		for (final Address cosignerAddress : multisigAddress.getMultisigLinks().getCosignatories()) {
			// TODO 20150103 J-G: what is the purpose of this check
			if (cosignerAddress.equals(transaction.getSigner().getAddress())) {
				continue;
			}

			// TODO 20150103 J-G: you can probably just use equals
			if (accountForRemoval != null && cosignerAddress.equals(accountForRemoval)) {
				continue;
			}

			// TODO 20150103 J-G: you can probably check t.getOtherTransactionHash().equals(transactionHash) outside of this loop
			final boolean hasCosigner = transaction.getCosignerSignatures().stream()
					.anyMatch(
							t -> t.getOtherTransactionHash().equals(transactionHash) &&
									t.getSigner().getAddress().equals(cosignerAddress)
					);

			if (!hasCosigner) {
				return ValidationResult.FAILURE_MULTISIG_MISSING_COSIGNERS;
			}
		}

		return ValidationResult.SUCCESS;
	}
}
