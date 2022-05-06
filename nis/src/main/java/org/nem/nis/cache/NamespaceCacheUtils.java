package org.nem.nis.cache;

import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.state.*;

import java.util.*;

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

	/**
	 * Get the collection of owners for a mosaic.
	 *
	 * @param cache The namespace cache.
	 * @param mosaicId The mosaic id.
	 * @return The collection of owners.
	 */
	public static Collection<Address> getMosaicOwners(final ReadOnlyNamespaceCache cache, final MosaicId mosaicId) {
		final ReadOnlyNamespaceEntry namespaceEntry = cache.get(mosaicId.getNamespaceId());
		if (null == namespaceEntry) {
			return Collections.emptyList();
		}

		final ReadOnlyMosaicEntry mosaicEntry = namespaceEntry.getMosaics().get(mosaicId);
		if (null == mosaicEntry) {
			return Collections.emptyList();
		}

		return mosaicEntry.getBalances().getOwners();
	}

	/**
	 * Gets all mosaic ids of for a given namespace id.
	 *
	 * @param cache The namespace cache.
	 * @param namespaceId The namespace id.
	 * @return The collection of mosaic ids.
	 */
	public static Collection<MosaicId> getMosaicIds(final ReadOnlyNamespaceCache cache, final NamespaceId namespaceId) {
		final ReadOnlyNamespaceEntry namespaceEntry = cache.get(namespaceId);
		if (null == namespaceEntry) {
			return Collections.emptyList();
		}

		return namespaceEntry.getMosaics().getMosaicIds();
	}
}
