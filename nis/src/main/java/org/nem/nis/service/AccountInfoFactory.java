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

	// TODO 20141014 J-G - can't this move to the caller (is it called more than once)?
	// > you can also add a function to the enum to do the mapping (RemoteStatus.toAccountRemoteStatus())
	// TODO 20141014 J-G - the tests you added are good, but i'd move them to RemoteStatus
	public AccountRemoteStatus getRemoteStatus(final Address address, final BlockHeight height) {
		final PoiAccountState accountState = this.poiFacade.findStateByAddress(address);
		final RemoteStatus remoteStatus = accountState.getRemoteLinks().getRemoteStatus(height);
		switch (remoteStatus) {
			case NOT_SET:
				return AccountRemoteStatus.INACTIVE;
			case OWNER_INACTIVE:
				return AccountRemoteStatus.INACTIVE;
			case OWNER_ACTIVATING:
				return AccountRemoteStatus.ACTIVATING;
			case OWNER_ACTIVE:
				return AccountRemoteStatus.ACTIVE;
			case OWNER_DEACTIVATING:
				return AccountRemoteStatus.DEACTIVATING;

			default:
				return AccountRemoteStatus.REMOTE;
		}
	}
}
