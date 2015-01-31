package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

/**
 * A transaction validator that checks transactions made from a multisig account as transaction signature presence:
 * a) transaction from a multisig account with a signature is not allowed
 * b) transaction from a multisig account without a signature is allowed (child transaction)
 * c) transaction from a non-multisig account with a signature is allowed
 * d) transaction from a non-multisig account without a signature is not allowed (child transaction)
 */
public class MultisigNonOperationalValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;

	/**
	 * Creates a new validator.
	 *
	 * @param stateCache The account state cache.
	 */
	public MultisigNonOperationalValidator(final ReadOnlyAccountStateCache stateCache) {
		this.stateCache = stateCache;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final ReadOnlyAccountState senderState = this.stateCache.findStateByAddress(transaction.getSigner().getAddress());

		// a non-multisig account must be signed (signature validation happens elsewhere)
		if (!senderState.getMultisigLinks().isMultisig()) {
			return isChildTransaction(transaction) ? ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE : ValidationResult.SUCCESS;
		}

		// once an account is multisig, it should not be allowed to make any other transactions
		return isChildTransaction(transaction)
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG;
	}

	private static boolean isChildTransaction(final Transaction transaction) {
		return null == transaction.getSignature();
	}
}
