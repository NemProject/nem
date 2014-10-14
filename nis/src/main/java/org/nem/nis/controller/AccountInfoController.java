package org.nem.nis.controller;

import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.AccountIdBuilder;
import org.nem.nis.harvesting.UnlockedAccounts;
import org.nem.nis.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for retrieving account related information
 */
@RestController
public class AccountInfoController {
	private final UnlockedAccounts unlockedAccounts;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final AccountInfoFactory accountInfoFactory;

	@Autowired(required = true)
	AccountInfoController(
			final UnlockedAccounts unlockedAccounts,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final AccountInfoFactory accountInfoFactory) {
		this.unlockedAccounts = unlockedAccounts;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.accountInfoFactory = accountInfoFactory;
	}

	/**
	 * Gets information about an account.
	 *
	 * @param builder The account id builder.
	 * @return The account information.
	 */
	@RequestMapping(value = "/account/get", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaDataPair accountGet(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		final Long height = this.blockChainLastBlockLayer.getLastBlockHeight();
		final AccountInfo account = this.accountInfoFactory.createInfo(address);
		final AccountRemoteStatus remoteStatus = this.accountInfoFactory.getRemoteStatus(address, new BlockHeight(height));
		final AccountMetaData metaData = new AccountMetaData(this.getAccountStatus(address), remoteStatus);
		return new AccountMetaDataPair(account, metaData);
	}

	@RequestMapping(value = "/account/status", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaData accountStatus(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		final Long height = this.blockChainLastBlockLayer.getLastBlockHeight();
		final AccountRemoteStatus remoteStatus = this.accountInfoFactory.getRemoteStatus(address, new BlockHeight(height));
		return new AccountMetaData(this.getAccountStatus(address), remoteStatus);
	}

	private AccountStatus getAccountStatus(final Address address) {
		return this.unlockedAccounts.isAccountUnlocked(address) ? AccountStatus.UNLOCKED : AccountStatus.LOCKED;
	}
}