package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

/**
 * This validator checks transactions made from a Multisig account
 * a) transaction inside MultisigTransaction, have signature == null, they are OK for multisig account
 * (this allows cosignatories to make actual transactions)
 * TODO 20150103 J-G: I just worry we're making this too complicated ... why allow any of the following?
 * > do you have time to come up with a list of multisig-related operations we want to support?
 * b) if signature is != null, that means TX was made DIRECTLY from multisig account, now:
 * b.1) multisig account can only make MultisigSignerModification,
 * b.2) or if multisig itself is also cosignatory of some other account, we allow MultisigSignatures too
 * b.3) if this was MultisigSignerModification we allow only Add
 */
public class MultisigNonOperationalValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;

	public MultisigNonOperationalValidator(final ReadOnlyAccountStateCache stateCache) {
		this.stateCache = stateCache;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		// not a multisig account, allow
		final ReadOnlyAccountState senderState = this.stateCache.findStateByAddress(transaction.getSigner().getAddress());
		if (!senderState.getMultisigLinks().isMultisig()) {
			return ValidationResult.SUCCESS;
		}

		// TODO 20141231: this is an evil hack to distinguish CHILD transaction from parent transaction
		// do not check child transaction, parent should be checked
		// TODO 20150103 J-G: what are you trying to do?
		if (transaction.getSignature() == null) {
			return ValidationResult.SUCCESS;
		}

		if (TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION != transaction.getType()) {
			if (senderState.getMultisigLinks().isCosignatory() && TransactionTypes.MULTISIG_SIGNATURE == transaction.getType()) {
				return ValidationResult.SUCCESS;
			}

			return ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG;
		}

		return this.validate((MultisigAggregateModificationTransaction)transaction, context);
	}

	private ValidationResult validate(final MultisigAggregateModificationTransaction transaction, final ValidationContext context) {
		// TODO: actually this should test if there is "Del"
		// TODO 20150103 J-G: should probably just test type validity
		final boolean invalid = transaction.getModifications().stream().anyMatch(m -> m.getModificationType() == MultisigModificationType.Del);
		return invalid ? ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG : ValidationResult.SUCCESS;
	}
}
