package org.nem.nis.controller;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.ncc.MosaicIdSupplyPair;
import org.nem.core.serialization.*;
import org.nem.nis.cache.*;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.controller.requests.MosaicIdBuilder;
import org.nem.nis.state.ReadOnlyMosaicEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST mosaic controller.
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
	 * Gets the mosaic id supply pair for a given mosaic id.
	 *
	 * @param builder The mosaic id builder.
	 * @return The mosaic id supply pair.
	 */
	@RequestMapping(value = "/mosaic/supply", method = RequestMethod.GET)
	@ClientApi
	public MosaicIdSupplyPair getMosaicSupply(final MosaicIdBuilder builder) {
		final MosaicId mosaicId = builder.build();
		return this.getPair(mosaicId);
	}

	// endregion

	// region getMosaicSupplyBatch

	/**
	 * Gets a list of mosaic id supply pairs for a given list of mosaic ids.
	 *
	 * @param deserializer The deserializer.
	 * @return The list of mosaic id supply pairs.
	 */
	@RequestMapping(value = "/mosaic/supply/batch", method = RequestMethod.POST)
	@ClientApi
	public SerializableList<MosaicIdSupplyPair> getMosaicSupplyBatch(final Deserializer deserializer) {
		final Collection<MosaicId> mosaicIds = new HashSet<>(new SerializableList<>(deserializer, MosaicId::new).asCollection());
		final Collection<MosaicIdSupplyPair> pairs = mosaicIds.stream().map(this::getPair).collect(Collectors.toList());
		return new SerializableList<>(pairs);
	}

	private MosaicIdSupplyPair getPair(final MosaicId mosaicId) {
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, mosaicId);
		if (null == entry) {
			throw new MissingResourceException(String.format("mosaic id %s is unknown", mosaicId.toString()),
					MosaicIdSupplyPair.class.getName(), mosaicId.toString());
		}

		return new MosaicIdSupplyPair(mosaicId, entry.getSupply());
	}

	// endregion
}
