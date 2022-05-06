package org.nem.nis.validators.transaction;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.validators.*;
import org.nem.nis.ForkConfiguration;

/**
 * A transaction validator that checks transactions made from a multisig account and checks signature presence:<br>
 * 1. transaction from a multisig account with a signature is not allowed<br>
 * 2. transaction from a multisig account without a signature is allowed (child transaction)
 */
public class MultisigNonOperationalValidator implements SingleTransactionValidator {
	private final ForkConfiguration forkConfiguration;
	private final ReadOnlyAccountStateCache stateCache;

	/**
	 * Creates a new validator.
	 *
	 * @param forkConfiguration The fork configuration.
	 * @param stateCache The account state cache.
	 */
	public MultisigNonOperationalValidator(final ForkConfiguration forkConfiguration, final ReadOnlyAccountStateCache stateCache) {
		this.forkConfiguration = forkConfiguration;
		this.stateCache = stateCache;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final ReadOnlyAccountState senderState = this.stateCache.findStateByAddress(transaction.getSigner().getAddress());

		// ignore non-multisig accounts (they must have signed transactions but signature validation happens elsewhere)
		if (!senderState.getMultisigLinks().isMultisig()) {
			return ValidationResult.SUCCESS;
		}

		// once an account is multisig, it should not be allowed to make any other transactions
		if (isChildTransaction(transaction)) {
			return ValidationResult.SUCCESS;
		}

		boolean isAllowedTransaction = false;
		if (this.forkConfiguration.getTreasuryReissuanceForkHeight().equals(context.getBlockHeight())) {
			final Hash transactionHash = HashUtils.calculateHash(transaction);
			isAllowedTransaction = this.forkConfiguration.getTreasuryReissuanceForkTransactionHashes().stream()
					.filter(hash -> hash.equals(transactionHash)).findAny().isPresent();
		}

		return isAllowedTransaction ? ValidationResult.SUCCESS : ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG;
	}

	private static boolean isChildTransaction(final Transaction transaction) {
		return null == transaction.getSignature();
	}
}
