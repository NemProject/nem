package org.nem.nis.controller;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.*;
import org.nem.nis.cache.*;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.state.ReadOnlyAccountState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for retrieving account namespace related information
 */
@RestController
public class AccountNamespaceInfoController {
	private final ReadOnlyAccountStateCache accountStateCache;
	private final ReadOnlyNamespaceCache namespaceCache;

	@Autowired(required = true)
	AccountNamespaceInfoController(
			final ReadOnlyAccountStateCache accountStateCache,
			final ReadOnlyNamespaceCache namespaceCache) {
		this.accountStateCache = accountStateCache;
		this.namespaceCache = namespaceCache;
	}

	/**
	 * Gets a list of mosaic definitions of mosaics owned by specified account.
	 *
	 * @param builder The account id builder.
	 * @return The list of mosaic definitions.
	 */
	@RequestMapping(value = "/account/mosaic/owned/definition", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<MosaicDefinition> accountGetMosaicDefinitions(final AccountIdBuilder builder) {
		return new SerializableList<>(this.getAccountMosaicDefinitions(builder.build()));
	}

	/**
	 * Gets a list of mosaic definitions of mosaics owned by all specified accounts.
	 *
	 * @param deserializer The deserializer.
	 * @return The list of mosaic definitions.
	 */
	@RequestMapping(value = "/account/mosaic/owned/definition/batch", method = RequestMethod.POST)
	@ClientApi
	public SerializableList<MosaicDefinition> accountGetMosaicDefinitionsBatch(@RequestBody final Deserializer deserializer) {
		final DeserializableList<AccountId> accounts = new DeserializableList<>(deserializer, AccountId::new);
		final Set<MosaicDefinition> allMosaics = new HashSet<>();
		for (final AccountId accountId : accounts.asCollection()) {
			allMosaics.addAll(this.getAccountMosaicDefinitions(accountId));
		}

		return new SerializableList<>(allMosaics);
	}

	/**
	 * Gets a list of mosaics (name and amount) owned by specified account.
	 *
	 * @param builder The account id builder.
	 * @return The list of mosaics.
	 */
	@RequestMapping(value = "/account/mosaic/owned", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Mosaic> accountGetOwnedMosaics(final AccountIdBuilder builder) {
		return new SerializableList<>(this.getAccountOwnedMosaics(builder.build()));
	}

	private Set<MosaicDefinition> getAccountMosaicDefinitions(final AccountId accountId) {
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(accountId.getAddress());
		final Set<MosaicDefinition> mosaicDefinitions = accountState.getAccountInfo().getMosaicIds().stream()
				.map(mosaicId -> this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId).getMosaicDefinition())
				.collect(Collectors.toSet());
		mosaicDefinitions.add(MosaicConstants.MOSAIC_DEFINITION_XEM);
		return mosaicDefinitions;
	}

	private List<Mosaic> getAccountOwnedMosaics(final AccountId accountId) {
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(accountId.getAddress());
		return accountState.getAccountInfo().getMosaicIds().stream()
				.map(mosaicId -> this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId))
				.map(entry -> new Mosaic(
						entry.getMosaicDefinition().getId(),
						entry.getBalances().getBalance(accountState.getAddress())))
				.collect(Collectors.toList());
	}
}
