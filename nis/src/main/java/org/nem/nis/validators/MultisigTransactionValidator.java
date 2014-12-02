package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.poi.PoiFacade;

public class MultisigTransactionValidator implements SingleTransactionValidator {
    private final PoiFacade poiFacade;

    public MultisigTransactionValidator(final PoiFacade poiFacade) {
        this.poiFacade = poiFacade;
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
        final PoiAccountState cosignerState = this.poiFacade.findStateByAddress(transaction.getSigner().getAddress());

        if (!cosignerState.getMultisigLinks().isCosignatoryOf(transaction.getOtherTransaction().getSigner().getAddress())) {
            return ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER;
        }

        return ValidationResult.SUCCESS;
    }
}
