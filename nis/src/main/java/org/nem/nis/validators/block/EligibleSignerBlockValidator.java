package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.validators.BlockValidator;

/**
 * Block validator that ensures the block signer is valid.
 */
public class EligibleSignerBlockValidator implements BlockValidator {
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a new validator.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public EligibleSignerBlockValidator(final ReadOnlyAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public ValidationResult validate(final Block block) {
		final Address signer = block.getSigner().getAddress();
		if (BlockedHarvesterPublicKeys.contains(signer.getPublicKey())) {
			return ValidationResult.FAILURE_CANNOT_HARVEST_FROM_BLOCKED_ACCOUNT;
		}

		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(signer);
		final RemoteStatus remoteStatus = accountState.getRemoteLinks().getRemoteStatus(block.getHeight());

		switch (remoteStatus) {
			case NOT_SET:
			case OWNER_ACTIVATING:
			case OWNER_INACTIVE:
			case REMOTE_DEACTIVATING:
			case REMOTE_ACTIVE:
				return ValidationResult.SUCCESS;
			default :
				break;
		}

		return ValidationResult.FAILURE_INELIGIBLE_BLOCK_SIGNER;
	}
}
