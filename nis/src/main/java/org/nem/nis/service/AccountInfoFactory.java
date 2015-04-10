package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.core.model.ncc.AccountInfo;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.poi.GroupedHeight;
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

	/**
	 * Creates a new account info factory.
	 *
	 * @param accountLookup The account lookup.
	 * @param accountStateCache The account state cache.
	 */
	@Autowired(required = true)
	public AccountInfoFactory(
			final AccountLookup accountLookup,
			final ReadOnlyAccountStateCache accountStateCache) {
		this.accountLookup = accountLookup;
		this.accountStateCache = accountStateCache;
	}

	/**
	 * Creates an account info for a specified account.
	 *
	 * @param address The account address.
	 * @return The info.
	 */
	public AccountInfo createInfo(final Address address) {
		final Account account = this.accountLookup.findByAddress(address);
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		final ReadOnlyAccountInfo accountInfo = accountState.getAccountInfo();

		final ReadOnlyAccountImportance ai = accountState.getImportanceInfo();
		return new AccountInfo(
				account.getAddress(),
				accountInfo.getBalance(),
				ai.isSet() ? accountState.getWeightedBalances().getVested(ai.getHeight()) : Amount.ZERO,
				accountInfo.getHarvestedBlocks(),
				accountInfo.getLabel(),
				!ai.isSet() ? 0.0 : ai.getImportance(ai.getHeight()));
	}

	/**
	 * Creates an account info for a specified account and a specified height.
	 *
	 * @param address The account address.
	 * @param height The block height.
	 * @return The info.
	 */
	public AccountInfo createInfo(final Address address, final BlockHeight height) {
		final Account account = this.accountLookup.findByAddress(address);
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		final ReadOnlyWeightedBalances weightedBalances = accountState.getWeightedBalances();
		final Amount vested = weightedBalances.getVested(height);
		final Amount unvested = weightedBalances.getUnvested(height);
		final ReadOnlyHistoricalImportances importances = accountState.getHistoricalImportances();

		final ReadOnlyAccountImportance ai = accountState.getImportanceInfo();
		return new AccountInfo(
				account.getAddress(),
				vested.add(unvested),
				vested,
				new BlockAmount(0),
				accountState.getAccountInfo().getLabel(),
				importances.getHistoricalImportance(GroupedHeight.fromHeight(height)));
	}
}
