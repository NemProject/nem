package org.nem.nis.cache;

import org.nem.core.model.mosaic.*;
import org.nem.nis.state.*;

/**
 * An adapter that adapts ReadOnlyNamespaceCache to MosaicFeeInformationLookup.
 */
public class NamespaceCacheToMosaicFeeInformationLookupAdapter implements MosaicFeeInformationLookup {
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates an adapter.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public NamespaceCacheToMosaicFeeInformationLookupAdapter(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public MosaicFeeInformation findById(final MosaicId id) {
		final ReadOnlyMosaicEntry entry = NamespaceCacheUtils.getMosaicEntry(this.namespaceCache, id);
		return null == entry
				? null
				: new MosaicFeeInformation(
				entry.getSupply(),
				entry.getMosaicDefinition().getProperties().getDivisibility(),
				entry.getMosaicDefinition().getTransferFeeInfo());
	}
}
