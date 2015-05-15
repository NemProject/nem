package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for creating account info models.
 */
@Service
public class AccountInfoFactory {
	private final AccountLookup accountLookup;
	private final ReadOnlyAccountStateCache accountStateCache;
	private final BlockChainLastBlockLayer lastBlockLayer;

	/**
	 * Creates a new account info factory.
	 *
	 * @param accountLookup The account lookup.
	 * @param accountStateCache The account state cache.
	 */
	@Autowired(required = true)
	public AccountInfoFactory(
			final AccountLookup accountLookup,
			final ReadOnlyAccountStateCache accountStateCache,
			final BlockChainLastBlockLayer lastBlockLayer) {
		this.accountLookup = accountLookup;
		this.accountStateCache = accountStateCache;
		this.lastBlockLayer = lastBlockLayer;
	}

	/**
	 * Creates an account info for a specified account.
	 *
	 * @param address The account address.
	 * @return The info.
	 */
	public AccountInfo createInfo(final Address address) {
		// TODO 20150515 BR -> J: what was the reason to look up the account and use the account's address vs. using the given address directly?
		final Account account = this.accountLookup.findByAddress(address);
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		final ReadOnlyAccountInfo accountInfo = accountState.getAccountInfo();

		final BlockHeight height = this.lastBlockLayer.getLastBlockHeight();
		final ReadOnlyAccountImportance ai = accountState.getImportanceInfo();
		return new AccountInfo(
				address,
				accountInfo.getBalance(),
				accountState.getWeightedBalances().getVested(height),
				accountInfo.getHarvestedBlocks(),
				accountInfo.getLabel(),
				!ai.isSet() ? 0.0 : ai.getImportance(ai.getHeight()));
	}
}
