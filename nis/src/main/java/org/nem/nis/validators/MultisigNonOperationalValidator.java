package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

public class MultisigNonOperationalValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;

	public MultisigNonOperationalValidator(final ReadOnlyAccountStateCache stateCache) {
		this.stateCache = stateCache;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		// not a multisig account, allow
		final ReadOnlyAccountState senderState = this.stateCache.findStateByAddress(transaction.getSigner().getAddress());
		if (! senderState.getMultisigLinks().isMultisig()) {
			return ValidationResult.SUCCESS;
		}

		// TODO 20141231: this is an evil hack to distinguish CHILD transaction from parent transaction
		// do not check child transaction, parent should be checked
		if (transaction.getSignature() == null) {
			return ValidationResult.SUCCESS;
		}

		if (TransactionTypes.MULTISIG_SIGNER_MODIFY != transaction.getType()) {
			if (senderState.getMultisigLinks().isCosignatory() && TransactionTypes.MULTISIG_SIGNATURE == transaction.getType()) {
				return ValidationResult.SUCCESS;
			}

			return ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG;
		}

		return this.validate((MultisigSignerModificationTransaction)transaction, context);
	}

	private ValidationResult validate(final MultisigSignerModificationTransaction transaction, final ValidationContext context) {
		// TODO: actually this should test if there is "Del"
		boolean invalid = transaction.getModifications().stream().anyMatch(m -> m.getModificationType() != MultisigModificationType.Add);
		return invalid ? ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG : ValidationResult.SUCCESS;
	}
}
