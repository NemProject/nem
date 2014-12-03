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

	private ValidationResult validate(final MultisigSignatureTransaction transaction, final ValidationContext context) {
		final PoiAccountState cosignerState = this.poiFacade.findStateByAddress(transaction.getSigner().getAddress());

		// iterate over "waiting"/current transactions, if there's no proper MultisigTransaction, validation fails
		final List<Transaction> foo = this.transactionsSupplier.get().stream()
				.filter(t -> TransactionTypes.MULTISIG == t.getType())
				.collect(Collectors.toList());
		boolean hasMatchingMultisigTransaction = this.transactionsSupplier.get().stream()
				.filter(t -> TransactionTypes.MULTISIG == t.getType())
				.anyMatch(
						t -> ((MultisigTransaction) t).getOtherTransactionHash().equals(transaction.getOtherTransactionHash()) &&
								cosignerState.getMultisigLinks().isCosignatoryOf(((MultisigTransaction)t).getOtherTransaction().getSigner().getAddress()) &&
								// don't let the MultisigTransaction issuer to create Signature too
								! transaction.getSigner().equals(t.getSigner())
				);

		return hasMatchingMultisigTransaction ? ValidationResult.SUCCESS : ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG;
	}
}
