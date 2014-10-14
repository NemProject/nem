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
// TODO 20141014 G-J: hadn't got time yesterday :]
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
		return getAccountRemoteStatus(remoteLinks, height);
	}

	private AccountRemoteStatus getAccountRemoteStatus(final RemoteLinks self, final BlockHeight height) {
		AccountRemoteStatus accountRemoteStatus = AccountRemoteStatus.REMOTE;
		if (self.isEmpty()) {
			accountRemoteStatus = AccountRemoteStatus.INACTIVE;
		} else if (self.isHarvestingRemotely()) {
			final boolean isActivated = ImportanceTransferTransaction.Mode.Activate.value() == self.getCurrent().getMode();
			final long heightDiff = height.subtract(self.getCurrent().getEffectiveHeight());
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
