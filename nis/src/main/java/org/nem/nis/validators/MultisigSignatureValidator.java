package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.poi.PoiFacade;

public class MultisigSignatureValidator implements SingleTransactionValidator {
	private final PoiFacade poiFacade;

	public MultisigSignatureValidator(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
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

		// TODO 20141202: would be nice to have some more checks here, which implies, that maybe it'd be worth
		// to have inside MultisigSignatureTransaction multisigAccount instead of signature...
		if (! cosignerState.getMultisigLinks().isCosignatory()) {
			return ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER;
		}

		return ValidationResult.SUCCESS;
	}
}
