package org.nem.nis.state;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A writable mosaics container.
 */
public class Mosaics implements ReadOnlyMosaics {
	private final NamespaceId namespaceId;
	private final ConcurrentHashMap<MosaicId, MosaicEntry> hashMap = new ConcurrentHashMap<>();

	/**
	 * Creates a new mosaics container.
	 *
	 * @param namespaceId The namespace id of all mosaics in the container.
	 */
	public Mosaics(final NamespaceId namespaceId) {
		this.namespaceId = namespaceId;
	}

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
	 * Adds a mosaic definition object to the cache.
	 *
	 * @param mosaicDefinition The mosaic definition.
	 * @return The added mosaic entry.
	 */
	public MosaicEntry add(final MosaicDefinition mosaicDefinition) {
		final MosaicEntry entry = new MosaicEntry(mosaicDefinition);
		this.add(entry);
		return entry;
	}

	/**
	 * Gets the namespace id of all mosaics in this cache.
	 *
	 * @return The namespace id.
	 */
	public NamespaceId getNamespaceId() {
		return this.namespaceId;
	}

	/**
	 * Adds a mosaic entry to the cache.
	 *
	 * @param entry The mosaic entry to add.
	 */
	protected void add(final MosaicEntry entry) {
		final MosaicDefinition mosaicDefinition = entry.getMosaicDefinition();
		if (!this.namespaceId.equals(mosaicDefinition.getId().getNamespaceId())) {
			throw new IllegalArgumentException(String.format("attempting to add mosaic with mismatched namespace %s", mosaicDefinition));
		}

		final MosaicEntry original = this.hashMap.putIfAbsent(mosaicDefinition.getId(), entry);
		if (null != original) {
			throw new IllegalArgumentException(String.format("mosaic %s already exists in cache", mosaicDefinition));
		}
	}

	/**
	 * Removes a mosaic object from the cache.
	 *
	 * @param id The mosaic id.
	 * @return The removed mosaic entry.
	 */
	public MosaicEntry remove(final MosaicId id) {
		final MosaicEntry original = this.hashMap.remove(id);
		if (null == original) {
			throw new IllegalArgumentException(String.format("mosaic '%s' not found in cache", id));
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
		final Mosaics copy = new Mosaics(this.namespaceId);
		copy.hashMap.putAll(this.hashMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy())));
		return copy;
	}
}
