package org.nem.nis.cache;

import org.nem.core.model.mosaic.*;
import org.nem.nis.state.*;

/**
 * Static class containing namespace cache helper functions.
 */
public class NamespaceCacheUtils {

	/**
	 * Gets the specified mosaic definition from the cache.
	 *
	 * @param cache The namespace cache.
	 * @param mosaicId The mosaic id.
	 * @return The mosaic definition or null if not found.
	 */
	public static MosaicDefinition getMosaicDefinition(final ReadOnlyNamespaceCache cache, final MosaicId mosaicId) {
		final ReadOnlyMosaicEntry mosaicEntry = getMosaicEntry(cache, mosaicId);
		return null == mosaicEntry ? null : mosaicEntry.getMosaicDefinition();
	}

	/**
	 * Gets the specified mosaic entry from the cache.
	 *
	 * @param cache The namespace cache.
	 * @param mosaicId The mosaic id.
	 * @return The mosaic entry or null if not found.
	 */
	public static ReadOnlyMosaicEntry getMosaicEntry(final ReadOnlyNamespaceCache cache, final MosaicId mosaicId) {
		final ReadOnlyNamespaceEntry namespaceEntry = cache.get(mosaicId.getNamespaceId());
		if (null == namespaceEntry) {
			return null;
		}

		return namespaceEntry.getMosaics().get(mosaicId);
	}
}
