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
	 * Gets a list of mosaic definitions owned by specified account.
	 *
	 * @param builder The account id builder.
	 * @return The list of mosaic definitions.
	 */
	@RequestMapping(value = "/account/mosaic-definitions/get", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<MosaicDefinition> accountGetMosaicDefinitions(final AccountIdBuilder builder) {
		return new SerializableList<>(this.getAccountMosaicDefinitions(builder.build()));
	}

	/**
	 * Gets a list of mosaic definitions owned by all specified accounts.
	 *
	 * @param deserializer The deserializer.
	 * @return The list of mosaic definitions.
	 */
	@RequestMapping(value = "/account/mosaic-definitions/get/batch", method = RequestMethod.POST)
	@ClientApi
	public SerializableList<MosaicDefinition> accountGetMosaicDefinitionsBatch(@RequestBody final Deserializer deserializer) {
		final DeserializableList<AccountId> accounts = new DeserializableList<>(deserializer, AccountId::new);
		final Set<MosaicDefinition> allMosaics = new HashSet<>();
		for (final AccountId accountId : accounts.asCollection()) {
			allMosaics.addAll(this.getAccountMosaicDefinitions(accountId));
		}

		// TODO 20150830 J-G: i don't think we should always be adding this
		// > or rather, there's an inconsistency now between get and get/batch
		// > if we always want to return this, getAccountMosaicDefinitions seems
		// > like a better place to add it
		allMosaics.add(MosaicConstants.MOSAIC_DEFINITION_XEM);

		return new SerializableList<>(allMosaics);
	}

	/**
	 * Gets a list of mosaics (name and amount) owned by specified account.
	 *
	 * @param builder The account id builder.
	 * @return The list of mosaics.
	 */
	@RequestMapping(value = "/account/owned-mosaics/get", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Mosaic> accountGetOwnedMosaics(final AccountIdBuilder builder) {
		return new SerializableList<>(this.getAccountOwnedMosaics(builder.build()));
	}

	private Set<MosaicDefinition> getAccountMosaicDefinitions(final AccountId accountId) {
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(accountId.getAddress());
		return accountState.getAccountInfo().getMosaicIds().stream()
				.map(mosaicId -> this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId).getMosaicDefinition())
				.collect(Collectors.toSet());
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
