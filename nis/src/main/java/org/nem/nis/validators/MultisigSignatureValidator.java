package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

import java.util.Collection;
import java.util.function.Supplier;

public class MultisigSignatureValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;
	private final Supplier<Collection<Transaction>> transactionsSupplier;

	public MultisigSignatureValidator(final ReadOnlyAccountStateCache stateCache, final Supplier<Collection<Transaction>> transactionsSupplier) {
		this.stateCache = stateCache;
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
		final ReadOnlyAccountState cosignerState = this.stateCache.findStateByAddress(signatureTransaction.getSigner().getAddress());

		// iterate over "waiting"/current transactions, if there's no proper MultisigTransaction, validation fails
		for (final Transaction parentTransaction : this.transactionsSupplier.get()) {
			if (TransactionTypes.MULTISIG != parentTransaction.getType()) {
				continue;
			}

			// TODO 20150103 J-G: why is this check needed?
			final MultisigTransaction multisigTransaction = (MultisigTransaction)parentTransaction;
			if (signatureTransaction.getSigner().equals(multisigTransaction.getSigner())) {
				continue;
			}

			if (!multisigTransaction.getOtherTransactionHash().equals(signatureTransaction.getOtherTransactionHash())) {
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
				// TODO 20150103 J-G: when a multisig transaction is removed, can't we remove all of its related signature transactions?
				// TODO 20150103 J-G: you should also test tthat the signature was added in the tests

				return ValidationResult.SUCCESS;
			}
		}

		return ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG;
	}
}
