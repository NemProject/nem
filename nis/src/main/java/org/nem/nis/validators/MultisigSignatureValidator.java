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
	private final boolean blockCreation;
	private final Supplier<Collection<Transaction>> transactionsSupplier;

	public MultisigSignatureValidator(final PoiFacade poiFacade, boolean blockCreation, final Supplier<Collection<Transaction>> transactionsSupplier) {
		this.poiFacade = poiFacade;
		this.blockCreation = blockCreation;
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
		if (this.blockCreation) {
			throw new RuntimeException("MultisigSignature not allowed during block creation");
		}

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

				// TODO: this most likely shouldn't be here... the following are wanted features:
				// a) MultisigSignatureTransaction should be added to UnconfirmedTransactions
				//    (I mean hash, so that we won't process one and the same TX multiple times)
				// b) MultisigSignature shouldn't be returned when asked for UTs for block
				//    (that's done by additional filter in UT.getTransactionsBefore)
				// c) yet it should be removed from UTs, when MultisigTransaction itself is removed
				//    not yet sure where
				multisigTransaction.addSignature(signatureTransaction);

				return ValidationResult.SUCCESS;
			}
		}

		return ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG;
	}
}
