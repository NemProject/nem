package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.*;
import org.nem.nis.secret.BlockChainConstants;

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

	private static boolean isRemoteActivated(final RemoteLinks remoteLinks) {
		return ImportanceTransferTransaction.Mode.Activate.value() == remoteLinks.getCurrent().getMode();
	}

	@Override
	public ValidationResult validate(final Block block) {
		final PoiAccountState accountState = this.poiFacade.findStateByAddress(block.getSigner().getAddress());
		final RemoteLinks remoteLinks = accountState.getRemoteLinks();
		if (remoteLinks.isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		// currently we can only have Activate and Deactivate, so we're ok to use single boolean for this
		final boolean isActivated = isRemoteActivated(remoteLinks);
		final long heightDiff = block.getHeight().subtract(remoteLinks.getCurrent().getEffectiveHeight());
		final boolean withinOneDay = heightDiff < BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
		if (remoteLinks.isHarvestingRemotely()) {
			if ((isActivated && withinOneDay) || (!isActivated && !withinOneDay)) {
				return ValidationResult.SUCCESS;
			}
		} else {
			if ((isActivated && !withinOneDay) || (!isActivated && withinOneDay)) {
				return ValidationResult.SUCCESS;
			}
		}

		return ValidationResult.FAILURE_ENTITY_UNUSABLE;
	}
}