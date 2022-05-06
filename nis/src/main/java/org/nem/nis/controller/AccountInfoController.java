package org.nem.nis.controller;

import org.nem.core.crypto.PublicKey;
import org.nem.core.model.Address;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.NodeFeature;
import org.nem.core.serialization.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.AccountHistoricalDataViewModel;
import org.nem.nis.pox.poi.GroupedHeight;
import org.nem.nis.service.*;
import org.nem.nis.state.*;
import org.nem.specific.deploy.NisConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for retrieving account related information
 */
@RestController
public class AccountInfoController {
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final AccountInfoFactory accountInfoFactory;
	private final AccountMetaDataFactory accountMetaDataFactory;
	private final ReadOnlyAccountStateCache accountStateCache;
	private final NisConfiguration nisConfiguration;

	@Autowired(required = true)
	AccountInfoController(final BlockChainLastBlockLayer blockChainLastBlockLayer, final AccountInfoFactory accountInfoFactory,
			final AccountMetaDataFactory accountMetaDataFactory, final ReadOnlyAccountStateCache accountStateCache,
			final NisConfiguration nisConfiguration) {
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.accountInfoFactory = accountInfoFactory;
		this.accountMetaDataFactory = accountMetaDataFactory;
		this.accountStateCache = accountStateCache;
		this.nisConfiguration = nisConfiguration;
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
		return this.getMetaDataPair(address);
	}

	/**
	 * Gets information about an account following all forwards.
	 *
	 * @param builder The account id builder.
	 * @return The (forwarded) account information.
	 */
	@RequestMapping(value = "/account/get/forwarded", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaDataPair accountGetForwarded(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		return this.getForwardedMetaDataPair(address);
	}

	/**
	 * Gets information about an account.
	 *
	 * @param builder The public key builder.
	 * @return The account information.
	 */
	@RequestMapping(value = "/account/get/from-public-key", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaDataPair accountGetFromPublicKey(final PublicKeyBuilder builder) {
		final PublicKey publicKey = builder.build();
		return this.getMetaDataPair(Address.fromPublicKey(publicKey));
	}

	/**
	 * Gets information about an account following all forwards.
	 *
	 * @param builder The public key builder.
	 * @return The (forwarded) account information.
	 */
	@RequestMapping(value = "/account/get/forwarded/from-public-key", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaDataPair accountGetForwardedFromPublicKey(final PublicKeyBuilder builder) {
		final PublicKey publicKey = builder.build();
		return this.getForwardedMetaDataPair(Address.fromPublicKey(publicKey));
	}

	/**
	 * Gets a list of account information.
	 *
	 * @param deserializer The deserializer.
	 * @return The list of account information.
	 */
	@RequestMapping(value = "/account/get/batch", method = RequestMethod.POST)
	@ClientApi
	public SerializableList<AccountMetaDataPair> accountGetBatch(@RequestBody final Deserializer deserializer) {
		final DeserializableList<AccountId> accounts = new DeserializableList<>(deserializer, AccountId::new);
		final Collection<AccountMetaDataPair> pairs = accounts.asCollection().stream().map(a -> this.getMetaDataPair(a.getAddress()))
				.collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	/**
	 * Gets historical information about an account.
	 *
	 * @param builder The account id builder.
	 * @return The account information.
	 */
	@RequestMapping(value = "/account/historical/get", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<AccountHistoricalDataViewModel> accountHistoricalDataGet(final AccountHistoricalDataRequestBuilder builder) {
		if (!this.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)) {
			throw new UnsupportedOperationException("this node does not support historical account data retrieval");
		}

		final AccountHistoricalDataRequest request = builder.build();
		final long endHeight = Math.min(request.getEndHeight().getRaw(), this.blockChainLastBlockLayer.getLastBlockHeight().getRaw());
		final List<AccountHistoricalDataViewModel> views = new ArrayList<>();
		for (long i = request.getStartHeight().getRaw(); i <= endHeight; i += request.getIncrement()) {
			views.add(this.getAccountHistoricalData(request.getAddress(), new BlockHeight(i)));
		}

		return new SerializableList<>(views);
	}

	/**
	 * Gets historical information about a collection of accounts.
	 *
	 * @param deserializer The deserializer.
	 * @return The account information.
	 */
	@RequestMapping(value = "/account/historical/get/batch", method = RequestMethod.POST)
	@ClientApi
	public SerializableList<SerializableList<AccountHistoricalDataViewModel>> accountHistoricalDataGetBatch(
			@RequestBody final Deserializer deserializer) {
		if (!this.nisConfiguration.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)) {
			throw new UnsupportedOperationException("this node does not support historical account data retrieval");
		}

		final AccountBatchHistoricalDataRequest request = new AccountBatchHistoricalDataRequest(deserializer);
		final long startHeight = request.getStartHeight().getRaw();
		final long endHeight = Math.min(request.getEndHeight().getRaw(), this.blockChainLastBlockLayer.getLastBlockHeight().getRaw());
		final List<SerializableList<AccountHistoricalDataViewModel>> viewsCollection = new ArrayList<>();
		for (AccountId accountId : request.getAccountIds()) {
			final List<AccountHistoricalDataViewModel> views = new ArrayList<>();
			for (long i = startHeight; i <= endHeight; i += request.getIncrement()) {
				views.add(this.getAccountHistoricalData(accountId.getAddress(), new BlockHeight(i)));
			}

			viewsCollection.add(new SerializableList<>(views));
		}

		return new SerializableList<>(viewsCollection);
	}

	@RequestMapping(value = "/account/status", method = RequestMethod.GET)
	@ClientApi
	public AccountMetaData accountStatus(final AccountIdBuilder builder) {
		final Address address = builder.build().getAddress();
		return this.accountMetaDataFactory.createMetaData(address);
	}

	private AccountMetaDataPair getMetaDataPair(final Address address) {
		final org.nem.core.model.ncc.AccountInfo accountInfo = this.accountInfoFactory.createInfo(address);
		final AccountMetaData metaData = this.accountMetaDataFactory.createMetaData(address);
		return new AccountMetaDataPair(accountInfo, metaData);
	}

	private AccountMetaDataPair getForwardedMetaDataPair(final Address address) {
		final ReadOnlyAccountState state = this.accountStateCache.findLatestForwardedStateByAddress(address);
		final org.nem.core.model.ncc.AccountInfo accountInfo = this.accountInfoFactory.createInfo(state.getAddress());
		final AccountMetaData metaData = this.accountMetaDataFactory.createMetaData(accountInfo.getAddress());
		return new AccountMetaDataPair(accountInfo, metaData);
	}

	private AccountHistoricalDataViewModel getAccountHistoricalData(final Address address, final BlockHeight height) {
		final BlockHeight groupedHeight = GroupedHeight.fromHeight(height);
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		final ReadOnlyWeightedBalances weightedBalances = accountState.getWeightedBalances();
		final Amount vested = weightedBalances.getVested(height);
		final Amount unvested = weightedBalances.getUnvested(height);
		final ReadOnlyHistoricalImportances importances = accountState.getHistoricalImportances();
		return new AccountHistoricalDataViewModel(height, address, vested.add(unvested), vested, unvested,
				importances.getHistoricalImportance(groupedHeight), importances.getHistoricalPageRank(groupedHeight));
	}
}
