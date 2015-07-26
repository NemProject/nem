package org.nem.nis.cache;

import org.nem.core.model.mosaic.*;
import org.nem.nis.state.*;

/**
 * Static class containing namespace cache helper functions.
 */
public class NamespaceCacheUtils {

	/**
	 * Gets the specified mosaic from the cache.
	 *
	 * @param cache The namespace cache.
	 * @param mosaicId The mosaic id.
	 * @return The mosaic or null if not found.
	 */
	public static Mosaic getMosaic(final ReadOnlyNamespaceCache cache, final MosaicId mosaicId) {
		final ReadOnlyMosaicEntry mosaicEntry = getMosaicEntry(cache, mosaicId);
		return null == mosaicEntry ? null : mosaicEntry.getMosaic();
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
