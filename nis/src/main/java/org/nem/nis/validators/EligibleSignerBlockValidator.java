package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.poi.*;

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

	// TODO 20140920 J-G: should we have more specific results?
	// TODO 20140920 J-G: also, i think this is the logic you wanted, but feel free to correct it if i'm wrong
	@Override
	public ValidationResult validate(final Block block) {
		final PoiAccountState accountState = this.poiFacade.findStateByAddress(block.getSigner().getAddress());

		// don't allow an account harvesting with a remote account to also harvest
		return accountState.getRemoteLinks().isHarvestingRemotely()
				? ValidationResult.FAILURE_ENTITY_UNUSABLE
				: ValidationResult.SUCCESS;
	}
}