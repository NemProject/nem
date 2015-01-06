package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.*;

/**
 * DbBlock validator that ensures the block signer is valid.
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
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(block.getSigner().getAddress());
		final RemoteStatus remoteStatus = accountState.getRemoteLinks().getRemoteStatus(block.getHeight());

		switch (remoteStatus) {
			case NOT_SET:
			case OWNER_ACTIVATING:
			case OWNER_INACTIVE:
			case REMOTE_DEACTIVATING:
			case REMOTE_ACTIVE:
				return ValidationResult.SUCCESS;
		}

		return ValidationResult.FAILURE_ENTITY_UNUSABLE;
	}
}