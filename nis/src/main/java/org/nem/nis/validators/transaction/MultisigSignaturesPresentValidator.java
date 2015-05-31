package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.validators.ValidationContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A transaction validator that validates that a multisig transaction has all required signatures.
 * - A non-delete aggregate modification (or any other supported transaction) is signed by at least minCosignatories cosigners.
 * - A delete aggregate modification is signed by all cosigners except the cosigner being deleted (when at least one cosigner remains).
 * - A delete aggregate modification of the last cosigner is signed by that cosigner.
 * - A delete aggregate modification can delete at most one account.
 * <br>
 * This validator should only be used during block creation, or when receiving a block.
 */
public class MultisigSignaturesPresentValidator implements TSingleTransactionValidator<MultisigTransaction> {
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
	public ValidationResult validate(final MultisigTransaction transaction, final ValidationContext context) {
		final ReadOnlyAccountState multisigState = this.stateCache.findStateByAddress(transaction.getOtherTransaction().getSigner().getAddress());
		final ReadOnlyMultisigLinks multisigLinks = multisigState.getMultisigLinks();

		final HashSet<Address> signerAddresses = new HashSet<>();
		signerAddresses.add(transaction.getSigner().getAddress());

		// TODO 20150531 BR -> all: there is no check if the signature signer is a cosignatory, so we need to check that too, right?.
		// note that we don't need to revalidate signatures here because a multisig signature transaction
		// does not allow the addition of invalid signatures
		signerAddresses.addAll(transaction.getCosignerSignatures().stream().map(s -> s.getSigner().getAddress()).collect(Collectors.toList()));
		if (!signerAddresses.stream().allMatch(address -> multisigLinks.getCosignatories().contains(address))) {
			return ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS;
		}

		final List<Address> accountsForRemoval = getRemovedAddresses(transaction);
		final Set<Address> expectedSignerAddresses = new HashSet<>(multisigLinks.getCosignatories());
		for (final Address accountForRemoval : accountsForRemoval) {
			signerAddresses.remove(accountForRemoval);
			expectedSignerAddresses.remove(accountForRemoval);
		}

		for (final Address address : signerAddresses) {
			expectedSignerAddresses.remove(address);
		}

		if (accountsForRemoval.isEmpty()) {
			return expectedSignerAddresses.size() <= multisigLinks.getCosignatories().size() - multisigLinks.minCosignatories()
					? ValidationResult.SUCCESS
					: ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS;
		} else {
			return expectedSignerAddresses.isEmpty()
					? ValidationResult.SUCCESS
					: ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS;
		}
	}

	private static List<Address> getRemovedAddresses(final MultisigTransaction transaction) {
		if (TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION != transaction.getOtherTransaction().getType()) {
			return Collections.emptyList();
		}

		final MultisigAggregateModificationTransaction modificationTransaction = (MultisigAggregateModificationTransaction)transaction.getOtherTransaction();
		return modificationTransaction.getCosignatoryModifications().stream()
				.filter(m -> m.getModificationType() == MultisigModificationType.DelCosignatory)
				.map(m -> m.getCosignatory().getAddress())
				.collect(Collectors.toList());
	}
}
