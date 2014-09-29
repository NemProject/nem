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

	@Override
	public ValidationResult validate(final Block block) {
		final PoiAccountState accountState = this.poiFacade.findStateByAddress(block.getSigner().getAddress());
		final RemoteLinks remoteLinks = accountState.getRemoteLinks();
		if (remoteLinks.isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		// currently we can only have Activate and Deactivate, so we're ok to use single boolean for this
		// TODO 20140927 J-G i wonder if it would be clearer to rename withinOneDay to something like isLastActivateChangeInEffect / isActivatedStatusInEffect
		// > i also wonder if, as a spam deterrent, isHarvestingRemotely && isActivated && withinOneDay should fail
		// G-J I don't think so, as a) if you activate, once it actually become active, you'll have to deactivate, so I'm not sure if such spamming
		//   would do anything... I think it'd be easier to spam synching
		//   b) probably most hosts shouldn't allow more than 1 "unlock"
		//   c) we should probably add "pruning" of unlocked accounts (probably before launch)
		// > (also, i realized that findForwardedStateByAddress is not appropriate because it is actually doing the opposite lookup)
		// G-J yeah, I was thinking if it would be possible to reuse it, but, the way it's done below, I think it's pretty understandable
		//   and doesn't look bad
		// > and thanks for all the test cases :)
		// P.S. my c/cpp nature screamed to make some evil XORs below, but I think, the way it is, it's much easier to read ;)
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

	// TODO 20140927 J-G (minor pedantic comment) i would move this below validate because i usually try to have calling functions before callee functions
	// TODO 20140929 G-J: sure thing it's some bad C (yes, C) habit, that I have
	private static boolean isRemoteActivated(final RemoteLinks remoteLinks) {
		return ImportanceTransferTransaction.Mode.Activate.value() == remoteLinks.getCurrent().getMode();
	}
}