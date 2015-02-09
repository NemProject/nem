package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.ReadOnlyAccountStateCache;

/**
 * A transaction validator that checks transactions that affect remote accounts:
 * a) the remote account can never be a signer of any transaction
 * b) the remote account can never be included in any non importance transfer
 * TODO 20150204 J-G: do we need a similar block validator?
 */
public class RemoteNonOperationalValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;

	/**
	 * Creates a new validator.
	 *
	 * @param stateCache The account state cache.
	 */
	public RemoteNonOperationalValidator(final ReadOnlyAccountStateCache stateCache) {
		this.stateCache = stateCache;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (this.isRemote(transaction.getSigner())) {
			return ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE;
		}

		if (BlockMarkerConstants.BETA_REMOTE_VALIDATION_FORK > context.getBlockHeight().getRaw()) {
			return ValidationResult.SUCCESS;
		}

		if (TransactionTypes.IMPORTANCE_TRANSFER == transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		return transaction.getAccounts().stream().filter(a -> !a.equals(transaction.getSigner())).anyMatch(this::isRemote)
				? ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE
				: ValidationResult.SUCCESS;
	}

	private boolean isRemote(final Account account) {
		return this.stateCache.findStateByAddress(account.getAddress()).getRemoteLinks().isRemoteHarvester();
	}
}