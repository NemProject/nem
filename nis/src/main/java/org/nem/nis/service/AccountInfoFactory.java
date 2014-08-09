package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.secret.AccountImportance;
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
		final AccountImportance ai = this.poiFacade.findStateByAddress(address).getImportanceInfo();
		return new AccountInfo(
				account.getAddress(),
				account.getBalance(),
				account.getForagedBlocks(),
				account.getLabel(),
				!ai.isSet() ? 0.0 : ai.getImportance(ai.getHeight()));
	}
}
