package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

/**
 * Block validator that ensures the block signer is valid.
 */
public class EligibleSignerBlockValidator implements BlockValidator {
	private final ReadOnlyAccountStateRepository accountStateRepository;

	/**
	 * Creates a new validator.
	 *
	 * @param accountStateRepository The poi facade.
	 */
	public EligibleSignerBlockValidator(final ReadOnlyAccountStateRepository accountStateRepository) {
		this.accountStateRepository = accountStateRepository;
	}

	@Override
	public ValidationResult validate(final Block block) {
		final ReadOnlyAccountState accountState = this.accountStateRepository.findStateByAddress(block.getSigner().getAddress());
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