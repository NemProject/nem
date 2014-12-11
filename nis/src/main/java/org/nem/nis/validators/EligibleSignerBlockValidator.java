package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.cache.PoiFacade;
import org.nem.nis.poi.*;
import org.nem.nis.remote.RemoteStatus;

/**
 * Block validator that ensures the block signer is valid.
 */
public class EligibleSignerBlockValidator implements BlockValidator {
	private final PoiFacade poiFacade;

	/**
	 * Creates a new validator.
	 *
	 * @param poiFacade The poi facade.
	 */
	public EligibleSignerBlockValidator(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
	}

	@Override
	public ValidationResult validate(final Block block) {
		final PoiAccountState accountState = this.poiFacade.findStateByAddress(block.getSigner().getAddress());
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