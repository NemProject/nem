package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.ValidationContext;

/**
 * A TransferTransactionValidator implementation that applies to importance transfer transactions.
 */
public class ImportanceTransferTransactionValidator implements TSingleTransactionValidator<ImportanceTransferTransaction> {
	private final ReadOnlyAccountStateCache accountStateCache;
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates a new validator.
	 *
	 * @param accountStateCache The account state cache.
	 * @param namespaceCache The namespace cache.
	 */
	public ImportanceTransferTransactionValidator(final ReadOnlyAccountStateCache accountStateCache,
			final ReadOnlyNamespaceCache namespaceCache) {
		this.accountStateCache = accountStateCache;
		this.namespaceCache = namespaceCache;
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
		return !remoteLinks.isEmpty() && height.subtract(remoteLinks.getCurrent().getEffectiveHeight()) < NemGlobals
				.getBlockChainConfiguration().getBlockChainRewriteLimit();
	}

	private ValidationResult validateOwner(final BlockHeight height, final ImportanceTransferTransaction transaction) {
		final ReadOnlyRemoteLinks remoteLinks = this.accountStateCache.findStateByAddress(transaction.getSigner().getAddress())
				.getRemoteLinks();
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
				// let's say I own account X which is harvesting and has big importance
				// user EVIL has small importance and announces remote harvesting, where he gives
				// X as his remote account
				// this basically cuts off X
				//
				// second attack vector, user X announces account Y as his remote
				// EVIL also announces Y as his remote... (handled by this.validateRemote and by BlockImportanceTransferValidator)
				// again this cuts off X from harvesting
				final ReadOnlyAccountState remoteAccountState = this.accountStateCache
						.findStateByAddress(transaction.getRemote().getAddress());
				final ReadOnlyAccountInfo remoteAccountInfo = remoteAccountState.getAccountInfo();
				if (0 != remoteAccountInfo.getBalance().compareTo(Amount.ZERO)) {
					return ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE;
				}

				// Remote Account Fork:
				// We also have to check that the remote account
				// - does not own any mosaic
				// - does not own any namespace
				// - is not a multisig account
				// - is not a cosignatory of any multsig account
				if (height.getRaw() >= BlockMarkerConstants.REMOTE_ACCOUNT_FORK(transaction.getVersion())) {
					if (!remoteAccountInfo.getMosaicIds().isEmpty()) {
						return ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE;
					}

					boolean ownsNamespace = this.namespaceCache.getRootNamespaceIds().stream().map(id -> {
						final ReadOnlyNamespaceEntry entry = this.namespaceCache.get(id);
						return entry.getNamespace().getOwner().equals(transaction.getRemote());
					}).reduce(false, Boolean::logicalOr);
					if (ownsNamespace) {
						return ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE;
					}

					if (remoteAccountState.getMultisigLinks().isMultisig()) {
						return ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE;
					}

					if (remoteAccountState.getMultisigLinks().isCosignatory()) {
						return ValidationResult.FAILURE_DESTINATION_ACCOUNT_IN_USE;
					}
				}

				// if a remote is already activated, it needs to be deactivated first
				return !isRemoteActivated(remoteLinks)
						? ValidationResult.SUCCESS
						: ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED;

			case Deactivate:
			default :
				// if a remote is already deactivated, it needs to be activated first
				return !isRemoteDeactivated(remoteLinks)
						? ValidationResult.SUCCESS
						: ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IS_NOT_ACTIVE;
		}
	}

	private ValidationResult validateRemote(final BlockHeight height, final ImportanceTransferTransaction transaction) {
		final ReadOnlyRemoteLinks remoteLinks = this.accountStateCache.findStateByAddress(transaction.getRemote().getAddress())
				.getRemoteLinks();

		// was the last importance transfer where the remote was involved less than rewrite limit blocks ago?
		if (isRemoteChangeWithinLimit(remoteLinks, height)) {
			return ValidationResult.FAILURE_IMPORTANCE_TRANSFER_IN_PROGRESS;
		}

		// if the remote account was not used as remote before, it is ok
		if (!remoteLinks.isRemoteHarvester()) {
			return ValidationResult.SUCCESS;
		}

		// else there is an owner. If that account is the transaction signer, it is ok
		final Address owner = remoteLinks.getCurrent().getLinkedAddress();
		if (owner.equals(transaction.getSigner().getAddress())) {
			// pass it, as rest will be checked in validateOwner
			return ValidationResult.SUCCESS;
		}

		final RemoteStatus remoteStatus = remoteLinks.getRemoteStatus(height);

		// A different owner can only be used if the old link is already deactivated
		switch (remoteStatus) {
			case REMOTE_ACTIVATING:
			case REMOTE_ACTIVE:
			case REMOTE_DEACTIVATING:
				return ValidationResult.FAILURE_IMPORTANCE_TRANSFER_NEEDS_TO_BE_DEACTIVATED;
			default :
				break;
		}

		return ValidationResult.SUCCESS;
	}
}
