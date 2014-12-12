package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for creating account info models.
 */
@Service
public class AccountInfoFactory {
	private final AccountLookup accountLookup;
	private final AccoutStateRepository accoutStateRepository;

	/**
	 * Creates a new account info factory.
	 *
	 * @param accountLookup The account lookup.
	 */
	@Autowired(required = true)
	public AccountInfoFactory(
			final AccountLookup accountLookup,
			final AccoutStateRepository accoutStateRepository) {
		this.accountLookup = accountLookup;
		this.accoutStateRepository = accoutStateRepository;
	}

	/**
	 * Creates an account info for a specified account.
	 *
	 * @param address The account address.
	 * @return The info.
	 */
	public AccountInfo createInfo(final Address address) {
		final Account account = this.accountLookup.findByAddress(address);
		final AccountState accountState = this.accoutStateRepository.findStateByAddress(address);
		final ReadOnlyAccountInfo accountInfo = accountState.getAccountInfo();

		final AccountImportance ai = accountState.getImportanceInfo();
		return new AccountInfo(
				account.getAddress(),
				accountInfo.getBalance(),
				accountInfo.getHarvestedBlocks(),
				accountInfo.getLabel(),
				!ai.isSet() ? 0.0 : ai.getImportance(ai.getHeight()));
	}
}
