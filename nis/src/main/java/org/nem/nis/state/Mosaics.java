package org.nem.nis.state;

import org.nem.core.model.mosaic.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A writable mosaics container.
 *
 * TODO 20150714 J-B: should we explicitly guard against cross-namespace mosaics?
 * TODO 20150716 BR -> J: though it should not happen a check would be good.
 */
public class Mosaics implements ReadOnlyMosaics {
	private final ConcurrentHashMap<MosaicId, MosaicEntry> hashMap = new ConcurrentHashMap<>();

	@Override
	public int size() {
		return this.hashMap.size();
	}

	@Override
	public MosaicEntry get(final MosaicId id) {
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
	 * @return The added mosaic entry.
	 */
	public MosaicEntry add(final Mosaic mosaic) {
		final MosaicEntry entry = new MosaicEntry(mosaic);
		final MosaicEntry original = this.hashMap.putIfAbsent(mosaic.getId(), entry);
		if (null != original) {
			throw new IllegalArgumentException(String.format("mosaic %s already exists in cache", mosaic.toString()));
		}

		return entry;
	}

	/**
	 * Removes a mosaic object from the cache.
	 *
	 * @param mosaic The mosaic.
	 * @return The removed mosaic entry.
	 */
	public MosaicEntry remove(final Mosaic mosaic) {
		final MosaicEntry original = this.hashMap.remove(mosaic.getId());
		if (null == original) {
			throw new IllegalArgumentException(String.format("mosaic '%s' not found in cache", mosaic.toString()));
		}

		return original;
	}

	/**
	 * Creates a copy of this container.
	 *
	 * @return A copy of this container.
	 */
	public Mosaics copy() {
		// note that mosaic ids are immutable
		final Mosaics copy = new Mosaics();
		copy.hashMap.putAll(this.hashMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy())));
		return copy;
	}
}
