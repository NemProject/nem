package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.poi.PoiFacade;

import java.util.Collection;
import java.util.function.Supplier;

public class MultisigSignaturesPresentValidator implements SingleTransactionValidator {
	private final PoiFacade poiFacade;
	private final boolean blockCreation;
	private final Supplier<Collection<Transaction>> transactionsSupplier;

	public MultisigSignaturesPresentValidator(final PoiFacade poiFacade, boolean blockCreation, final Supplier<Collection<Transaction>> transactionsSupplier) {
		this.poiFacade = poiFacade;
		this.blockCreation = blockCreation;
		this.transactionsSupplier = transactionsSupplier;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.MULTISIG != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		return this.validate((MultisigTransaction)transaction, context);
	}

	private ValidationResult validate(final MultisigTransaction transaction, final ValidationContext context) {
		final PoiAccountState senderState = this.poiFacade.findStateByAddress(transaction.getSigner().getAddress());

		if (! senderState.isCosignerOf(transaction.getOtherTransaction().getSigner().getAddress())) {
			return ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER;
		}

		// if this is not block creation, we don't want to check if signature of all cosignatories are present
		if (! blockCreation) {
			return ValidationResult.SUCCESS;
		}

		final PoiAccountState multisigAddress = this.poiFacade.findStateByAddress(transaction.getOtherTransaction().getSigner().getAddress());
		if (multisigAddress.getCosigners().size() == 1) {
			return ValidationResult.SUCCESS;
		}

		final Hash transactionHash = transaction.getOtherTransactionHash();
		// this loop could be done using reduce, but I'm leaving it like this for readability
		for (final Address cosignerAddress : multisigAddress.getCosigners()) {
			boolean hasCosigner = this.transactionsSupplier.get().stream()
					.filter(t -> TransactionTypes.MULTISIG_SIGNATURE == t.getType())
					.anyMatch(
							t -> ((MultisigSignatureTransaction) t).getOtherTransactionHash().equals(transactionHash) &&
									t.getSigner().getAddress().equals(cosignerAddress)
					);

			if (! hasCosigner) {
				return ValidationResult.FAILURE_MULTISIG_MISSING_COSIGNERS;
			}
		}

		return ValidationResult.SUCCESS;
	}
}
