package org.nem.nis.controller;

import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.*;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.service.MosaicInfoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for retrieving account namespace related information
 */
@RestController
public class AccountNamespaceInfoController {
	private final MosaicInfoFactory mosaicInfoFactory;

	@Autowired(required = true)
	AccountNamespaceInfoController(final MosaicInfoFactory mosaicInfoFactory) {
		this.mosaicInfoFactory = mosaicInfoFactory;
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
		final Address address = builder.build().getAddress();
		return new SerializableList<>(this.mosaicInfoFactory.getMosaicDefinitions(address));
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
			final Address address = accountId.getAddress();
			allMosaics.addAll(this.mosaicInfoFactory.getMosaicDefinitions(address));
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

	private List<Mosaic> getAccountOwnedMosaics(final AccountId accountId) {
		final Address address = accountId.getAddress();
		return this.mosaicInfoFactory.getAccountOwnedMosaics(address);
	}
}
