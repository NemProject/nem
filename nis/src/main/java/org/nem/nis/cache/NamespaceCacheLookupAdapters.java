package org.nem.nis.cache;

import org.nem.core.model.mosaic.*;
import org.nem.nis.state.ReadOnlyMosaicEntry;

import java.util.function.Function;

/**
 * Helper class that adapts ReadOnlyNamespaceCache to lookups used by nem.core.
 */
public class NamespaceCacheLookupAdapters {
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates adapters.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public NamespaceCacheLookupAdapters(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	/**
	 * Adapts a namespace cache to a MosaicFeeInformationLookup.
	 *
	 * @return A MosaicFeeInformationLookup.
	 */
	public MosaicFeeInformationLookup asMosaicFeeInformationLookup() {
		return id -> this.getEntityData(id,
				entry -> new MosaicFeeInformation(entry.getSupply(), entry.getMosaicDefinition().getProperties().getDivisibility()));
	}

	/**
	 * Adapts a namespace cache to a MosaicLevyLookup.
	 *
	 * @return A MosaicLevyLookup.
	 */
	public MosaicLevyLookup asMosaicLevyLookup() {
		return id -> this.getEntityData(id, entry -> entry.getMosaicDefinition().getMosaicLevy());
	}

	private <T> T getEntityData(final MosaicId id, final Function<ReadOnlyMosaicEntry, T> map) {
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, id);
		return null == entry ? null : map.apply(entry);
	}
}
