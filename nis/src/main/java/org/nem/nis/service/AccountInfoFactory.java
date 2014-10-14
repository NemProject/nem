package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.poi.*;
import org.nem.nis.secret.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for creating account info models.
 */
@Service
// TODO 20141013 G-J: not sure if getRemoteStatus should be here, and if so, this probably should be renamed
// TODO 20141013 J-G: i guess you didn't like it in RemoteLinks for some reason?
public class AccountInfoFactory {
	private final AccountLookup accountLookup;
	private final PoiFacade poiFacade;

	/**
	 * Creates a new account info factory.
	 *
	 * @param accountLookup The account lookup.
	 */
	@Autowired(required = true)
	public AccountInfoFactory(
			final AccountLookup accountLookup,
			final PoiFacade poiFacade) {
		this.accountLookup = accountLookup;
		this.poiFacade = poiFacade;
	}

	/**
	 * Creates an account info for a specified account.
	 *
	 * @param address The account address.
	 * @return The info.
	 */
	public AccountInfo createInfo(final Address address) {
		final Account account = this.accountLookup.findByAddress(address);
		final PoiAccountState accountState = this.poiFacade.findStateByAddress(address);

		final AccountImportance ai = accountState.getImportanceInfo();
		return new AccountInfo(
				account.getAddress(),
				account.getBalance(),
				account.getForagedBlocks(),
				account.getLabel(),
				!ai.isSet() ? 0.0 : ai.getImportance(ai.getHeight()));
	}

	public AccountRemoteStatus getRemoteStatus(final Address address, final BlockHeight height) {
		final PoiAccountState accountState = this.poiFacade.findStateByAddress(address);
		final RemoteLinks remoteLinks = accountState.getRemoteLinks();

		// TODO 20141005 J-G - i think i would prefer to have this logic in RemoteLinks - something like getRemoteState
		// TODO 20141013 J-G - can we reuse this function in other places too? (e.g. the validator and possibly poi facade)
		// TODO 20141013 J-G - definitely should test this function

		AccountRemoteStatus accountRemoteStatus = AccountRemoteStatus.REMOTE;
		if (remoteLinks.isEmpty()) {
			accountRemoteStatus = AccountRemoteStatus.INACTIVE;
		} else if (remoteLinks.isHarvestingRemotely()) {
			final boolean isActivated = ImportanceTransferTransaction.Mode.Activate.value() == remoteLinks.getCurrent().getMode();
			final long heightDiff = height.subtract(remoteLinks.getCurrent().getEffectiveHeight());
			final boolean withinOneDay = heightDiff < BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
			if (isActivated) {
				accountRemoteStatus = withinOneDay ? AccountRemoteStatus.ACTIVATING : AccountRemoteStatus.ACTIVE;
			} else {
				accountRemoteStatus = withinOneDay ? AccountRemoteStatus.DEACTIVATING : AccountRemoteStatus.INACTIVE;
			}
		}

		return accountRemoteStatus;
	}
}
