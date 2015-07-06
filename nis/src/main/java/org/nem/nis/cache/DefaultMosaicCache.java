package org.nem.nis.cache;

import org.nem.core.model.mosaic.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * General class for holding mosaics.
 */
public class DefaultMosaicCache implements MosaicCache, CopyableCache<DefaultMosaicCache> {
	private final ConcurrentHashMap<MosaicId, Mosaic> hashMap = new ConcurrentHashMap<>();

	// region ReadOnlyMosaicCache

	@Override
	public int size() {
		return this.hashMap.size();
	}

	@Override
	public Mosaic get(final MosaicId id) {
		return this.hashMap.get(id);
	}

	@Override
	public boolean contains(final MosaicId id) {
		return this.hashMap.containsKey(id);
	}

	// endregion

	// region MosaicCache

	@Override
	public void add(final Mosaic mosaic) {
		final Mosaic original = this.hashMap.putIfAbsent(mosaic.getId(), mosaic);
		if (null != original) {
			throw new IllegalArgumentException(String.format("mosaic %s already exists in cache", mosaic.toString()));
		}
	}

	@Override
	public void remove(final Mosaic mosaic) {
		final Mosaic original = this.hashMap.remove(mosaic.getId());
		if (null == original) {
			throw new IllegalArgumentException(String.format("mosaic '%s' not found in cache", mosaic.toString()));
		}
	}

	// endregion

	// region CopyableCache

	@Override
	public void shallowCopyTo(final DefaultMosaicCache rhs) {
		rhs.hashMap.clear();
		rhs.hashMap.putAll(this.hashMap);
	}

	@Override
	public DefaultMosaicCache copy() {
		// Mosaic objects are immutable
		final DefaultMosaicCache copy = new DefaultMosaicCache();
		copy.hashMap.putAll(this.hashMap);
		return copy;
	}

	// endregion
}
