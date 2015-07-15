package org.nem.nis.state;

import org.nem.core.model.mosaic.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A writable mosaics container.
 */
public class Mosaics implements ReadOnlyMosaics {
	private final ConcurrentHashMap<MosaicId, Mosaic> hashMap = new ConcurrentHashMap<>();

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

	/**
	 * Adds a mosaic object to the cache.
	 *
	 * @param mosaic The mosaic.
	 */
	public void add(final Mosaic mosaic) {
		final Mosaic original = this.hashMap.putIfAbsent(mosaic.getId(), mosaic);
		if (null != original) {
			throw new IllegalArgumentException(String.format("mosaic %s already exists in cache", mosaic.toString()));
		}
	}

	/**
	 * Removes a mosaic object from the cache.
	 *
	 * @param mosaic The mosaic.
	 */
	public void remove(final Mosaic mosaic) {
		final Mosaic original = this.hashMap.remove(mosaic.getId());
		if (null == original) {
			throw new IllegalArgumentException(String.format("mosaic '%s' not found in cache", mosaic.toString()));
		}
	}

	/**
	 * Creates a copy of this container.
	 *
	 * @return A copy of this container.
	 */
	public Mosaics copy() {
		// note that mosaic ids and mosaics are immutable
		final Mosaics copy = new Mosaics();
		copy.hashMap.putAll(this.hashMap);
		return copy;
	}
}
