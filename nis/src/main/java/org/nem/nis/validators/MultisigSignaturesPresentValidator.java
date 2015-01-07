package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A transaction validator that validates that a multisig transaction has all required signatures.
 * <br/>
 * This validator should only be used during block creation, or when receiving a block.
 */
public class MultisigSignaturesPresentValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;

	/**
	 * Creates a new validator.
	 *
	 * @param stateCache The account state cache.
	 */
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

		return this.validate((MultisigTransaction)transaction);
	}

	private ValidationResult validate(final MultisigTransaction transaction) {
		final ReadOnlyAccountState multisigAddress = this.stateCache.findStateByAddress(transaction.getOtherTransaction().getSigner().getAddress());

		final Hash transactionHash = transaction.getOtherTransactionHash();
		final HashSet<Address> signerAddresses = new HashSet<>();
		signerAddresses.add(transaction.getSigner().getAddress());
		for (final MultisigSignatureTransaction signature : transaction.getCosignerSignatures()) {
			if (!transactionHash.equals(signature.getOtherTransactionHash())) {
				return ValidationResult.FAILURE_MULTISIG_MISMATCHED_SIGNATURE;
			}

			signerAddresses.add(signature.getSigner().getAddress());
		}

		final List<Address> accountsForRemoval = getRemovedAddresses(transaction);
		if (accountsForRemoval.size() > 1) {
			return ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG;
		}

		final Address accountForRemoval = accountsForRemoval.isEmpty() ? null : accountsForRemoval.get(0);
		final Set<Address> expectedSignerAddresses = multisigAddress.getMultisigLinks().getCosignatories();
		if (null != accountForRemoval && expectedSignerAddresses.size() > 1) {
			signerAddresses.remove(accountForRemoval);
			expectedSignerAddresses.remove(accountForRemoval);
		}

		return signerAddresses.equals(expectedSignerAddresses)
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS;
	}

	private static List<Address> getRemovedAddresses(final MultisigTransaction transaction) {
		if (TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION != transaction.getOtherTransaction().getType()) {
			return Collections.emptyList();
		}

		final MultisigAggregateModificationTransaction modificationTransaction = (MultisigAggregateModificationTransaction)transaction.getOtherTransaction();
		return modificationTransaction.getModifications().stream()
				.filter(m -> m.getModificationType() == MultisigModificationType.Del)
				.map(m -> m.getCosignatory().getAddress())
				.collect(Collectors.toList());
	}
}
