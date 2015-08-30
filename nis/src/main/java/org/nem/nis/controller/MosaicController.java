package org.nem.nis.controller;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.nis.cache.*;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.MosaicIdBuilder;
import org.nem.nis.controller.viewmodels.MosaicIdSupplyPair;
import org.nem.nis.state.ReadOnlyMosaicEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST mosaic controller.
 * TODO 20150830 J-*: needs tests
 */
@RestController
public class MosaicController {
	private final ReadOnlyNamespaceCache namespaceCache;

	@Autowired(required = true)
	MosaicController(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	// region getMosaicSupply

	/**
	 * Gets the mosaic definition for a given mosaic id.
	 *
	 * @param builder The mosaic id builder.
	 * @return The mosaic definition.
	 */
	@RequestMapping(value = "/mosaic/supply", method = RequestMethod.GET)
	@ClientApi
	public MosaicIdSupplyPair getMosaicSupply(final MosaicIdBuilder builder) {
		final MosaicId mosaicId = builder.build();

		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, mosaicId);
		if (null == entry) {
			throw new IllegalArgumentException(String.format("mosaic id %s is unknown", mosaicId.toString()));
		}

		return new MosaicIdSupplyPair(mosaicId, entry.getSupply());
	}

	// endregion

}
