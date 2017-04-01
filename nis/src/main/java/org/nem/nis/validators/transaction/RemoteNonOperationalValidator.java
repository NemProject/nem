package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.validators.*;

/**
 * A transaction validator that checks transactions that affect remote accounts:
 * a) the remote account can never be a signer of any transaction
 * b) the remote account can never be included in any non importance transfer
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
		if (!this.isRemoteInActive(transaction.getSigner(), context.getBlockHeight())) {
			return ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE;
		}

		if (TransactionTypes.IMPORTANCE_TRANSFER == transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		return transaction.getAccounts().stream()
				.filter(a -> !a.equals(transaction.getSigner()))
				.anyMatch(a -> !this.isRemoteInActive(a, context.getBlockHeight()))
				? ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_REMOTE
				: ValidationResult.SUCCESS;
	}

	private boolean isRemoteInActive(final Account account, final BlockHeight height) {
		final ReadOnlyRemoteLinks remotelinks = this.stateCache.findStateByAddress(account.getAddress()).getRemoteLinks();
		final RemoteStatus status = remotelinks.getRemoteStatus(height);
		return !remotelinks.isRemoteHarvester() || RemoteStatus.NOT_SET == status || RemoteStatus.REMOTE_INACTIVE == status;
	}
}