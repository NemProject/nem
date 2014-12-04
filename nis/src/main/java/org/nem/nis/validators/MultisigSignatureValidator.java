package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.poi.PoiFacade;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MultisigSignatureValidator implements SingleTransactionValidator {
	private final PoiFacade poiFacade;
	private final Supplier<Collection<Transaction>> transactionsSupplier;

	public MultisigSignatureValidator(final PoiFacade poiFacade, final Supplier<Collection<Transaction>> transactionsSupplier) {
		this.poiFacade = poiFacade;
		this.transactionsSupplier = transactionsSupplier;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.MULTISIG_SIGNATURE != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		if (context.getBlockHeight().getRaw() < BlockMarkerConstants.BETA_MULTISIG_FORK) {
			return ValidationResult.FAILURE_ENTITY_UNUSABLE;
		}

		return this.validate((MultisigSignatureTransaction)transaction, context);
	}

	private ValidationResult validate(final MultisigSignatureTransaction signatureTransaction, final ValidationContext context) {
		final PoiAccountState cosignerState = this.poiFacade.findStateByAddress(signatureTransaction.getSigner().getAddress());

		// iterate over "waiting"/current transactions, if there's no proper MultisigTransaction, validation fails
		for (final Transaction parentTransaction : this.transactionsSupplier.get()) {
			if (TransactionTypes.MULTISIG != parentTransaction.getType()) {
				continue;
			}

			final MultisigTransaction multisigTransaction = (MultisigTransaction)parentTransaction;
			if (signatureTransaction.getSigner().equals(multisigTransaction.getSigner())) {
				continue;
			}

			if (! multisigTransaction.getOtherTransactionHash().equals(signatureTransaction.getOtherTransactionHash())) {
				return ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG;
			}

			if (cosignerState.getMultisigLinks().isCosignatoryOf(multisigTransaction.getOtherTransaction().getSigner().getAddress())) {
				return ValidationResult.SUCCESS;
			}
		}

		return ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG;
	}
}
