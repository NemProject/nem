package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.validators.ValidationContext;

/**
 * A TransferTransactionValidator implementation that applies to importance transfer transactions.
 */
public class ImportanceTransferTransactionValidator implements TSingleTransactionValidator<ImportanceTransferTransaction> {
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a new validator.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public ImportanceTransferTransactionValidator(final ReadOnlyAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public ValidationResult validate(final ImportanceTransferTransaction transaction, final ValidationContext context) {
		final ValidationResult result = this.validateRemote(context.getBlockHeight(), transaction);
		if (!result.isSuccess()) {
			return result;
		}

		return this.validateOwner(context.getBlockHeight(), transaction);
	}

	private static boolean isRemoteActivated(final ReadOnlyRemoteLinks remoteLinks) {
		return !remoteLinks.isEmpty() && ImportanceTransferMode.Activate == remoteLinks.getCurrent().getMode();
	}

	private static boolean isRemoteDeactivated(final ReadOnlyRemoteLinks remoteLinks) {
		return remoteLinks.isEmpty() || ImportanceTransferMode.Deactivate == remoteLinks.getCurrent().getMode();
	}

	private static boolean isRemoteChangeWithinLimit(final ReadOnlyRemoteLinks remoteLinks, final BlockHeight height) {
		return !remoteLinks.isEmpty() && height.subtract(remoteLinks.getCurrent().getEffectiveHeight()) < BlockChainConstants.REMOTE_HARVESTING_DELAY;
	}

	private ValidationResult validateOwner(final BlockHeight height, final ImportanceTransferTransaction transaction) {
		final ReadOnlyRemoteLinks remoteLinks = this.accountStateCache.findStateByAddress(transaction.getSigner().getAddress()).getRemoteLinks();
		if (isRemoteChangeWithinLimit(remoteLinks, height)) {
			return ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS;
		}

		switch (transaction.getMode()) {
			case Activate:
				// ONLY for remote harvesting, so we should probably block any incoming or outgoing transfers, additionally we
				// shouldn't allow setting an account that already have some balance on it.
				//
				// I finally have possible attack vector
				// (handled by check below, and partially by BlockImportanceTransferBalanceValidator):
				//   let's say I own account X which is harvesting and has big importance
				//   user EVIL has small importance and announces remote harvesting, where he gives
				//       X as his remote account
				//   this basically cuts off X
				//
				// second attack vector, user X announces account Y as his remote
				// EVIL also announces Y as his remote... (handled by this.validateRemote and by BlockImportanceTransferValidator)
				// again this cuts off X from harvesting
				final Amount remoteBalance = this.accountStateCache.findStateByAddress(transaction.getRemote().getAddress()).getAccountInfo().getBalance();
				if (0 != remoteBalance.compareTo(Amount.ZERO)) {
					return ValidationResult.FAILURE_DESTINATION_ACCOUNT_HAS_PREEXISTING_BALANCE_TRANSFER;
				}

				// if a remote is already activated, it needs to be deactivated first
				return !isRemoteActivated(remoteLinks) ? ValidationResult.SUCCESS : ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED;

			case Deactivate:
			default:
				// if a remote is already deactivated, it needs to be activated first
				return !isRemoteDeactivated(remoteLinks) ? ValidationResult.SUCCESS : ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE;
		}
	}

	private ValidationResult validateRemote(final BlockHeight height, final ImportanceTransferTransaction transaction) {
		final ReadOnlyRemoteLinks remoteLinks = this.accountStateCache.findStateByAddress(transaction.getRemote().getAddress()).getRemoteLinks();
		if (isRemoteChangeWithinLimit(remoteLinks, height)) {
			return ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS;
		}

		if (!remoteLinks.isRemoteHarvester()) {
			return ValidationResult.SUCCESS;
		}

		final Address owner = remoteLinks.getCurrent().getLinkedAddress();
		if (owner == transaction.getSigner().getAddress()) {
			// pass it, as rest will be checked in validateOwner
			return ValidationResult.SUCCESS;
		}

		final RemoteStatus remoteStatus = remoteLinks.getRemoteStatus(height);

		switch (remoteStatus) {
			case REMOTE_ACTIVATING:
			case REMOTE_ACTIVE:
			case REMOTE_DEACTIVATING:
				return ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED;
		}

		return ValidationResult.SUCCESS;
	}
}
