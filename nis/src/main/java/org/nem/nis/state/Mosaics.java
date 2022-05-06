package org.nem.nis.state;

import org.nem.core.model.NetworkInfos;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockMarkerConstants;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A writable mosaics container.
 */
public class Mosaics implements ReadOnlyMosaics {
	private final NamespaceId namespaceId;
	private final ConcurrentHashMap<MosaicId, MosaicEntryHistory> hashMap = new ConcurrentHashMap<>();

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
	public int deepSize() {
		return this.hashMap.values().stream().map(MosaicEntryHistory::historyDepth).reduce(0, Integer::sum);
	}

	@Override
	public MosaicEntry get(final MosaicId id) {
		final MosaicEntryHistory history = this.hashMap.get(id);
		return (null == history || history.isEmpty()) ? null : history.last();
	}

	@Override
	public Collection<MosaicId> getMosaicIds() {
		return Collections.unmodifiableCollection(this.hashMap.keySet());
	}

	@Override
	public boolean contains(final MosaicId id) {
		return this.hashMap.containsKey(id);
	}

	/**
	 * Adds a mosaic definition object to the mosaic history.
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
	 * Adds a mosaic definition object to the mosaic history.
	 *
	 * @param mosaicDefinition The mosaic definition.
	 * @param height The block chain height.
	 * @return The added mosaic entry.
	 */
	public MosaicEntry add(final MosaicDefinition mosaicDefinition, final BlockHeight height) {
		final MosaicEntry entry = new MosaicEntry(mosaicDefinition);
		this.add(entry, height);
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
	 * Adds a mosaic entry to the mosaic history.
	 *
	 * @param entry The mosaic entry to add.
	 */
	protected void add(final MosaicEntry entry) {
		this.add(entry, new BlockHeight(1));
	}

	/**
	 * Adds a mosaic entry to the mosaic history.
	 *
	 * @param entry The mosaic entry to add.
	 * @param height The block chain height.
	 */
	protected void add(final MosaicEntry entry, final BlockHeight height) {
		final MosaicDefinition mosaicDefinition = entry.getMosaicDefinition();
		if (!this.namespaceId.equals(mosaicDefinition.getId().getNamespaceId())) {
			throw new IllegalArgumentException(
					String.format("attempting to add mosaic definition with mismatched namespace %s", mosaicDefinition));
		}

		if (!this.hashMap.containsKey(mosaicDefinition.getId())) {
			this.hashMap.put(mosaicDefinition.getId(), new MosaicEntryHistory(entry));
			return;
		}

		final MosaicEntryHistory history = this.hashMap.get(mosaicDefinition.getId());
		if (height.getRaw() < BlockMarkerConstants.MOSAIC_REDEFINITION_FORK(NetworkInfos.getDefault().getVersion() << 24)) {
			history.push(entry);
			return;
		}

		// after fork: if only the description changed, we have to inherit the balances and the supply
		final MosaicEntry original = history.last();
		final MosaicDefinition originalDefinition = original.getMosaicDefinition();
		final MosaicProperties originalProperties = originalDefinition.getProperties();
		final MosaicProperties newProperties = mosaicDefinition.getProperties();
		if (originalProperties.equals(newProperties)
				&& Objects.equals(originalDefinition.getMosaicLevy(), mosaicDefinition.getMosaicLevy())) {
			final MosaicEntry newEntry = new MosaicEntry(mosaicDefinition, original.getSupply(), original.getBalances().copy());
			history.push(newEntry);
		} else {
			history.push(entry);
		}
	}

	/**
	 * Removes a mosaic object from the mosaic history.
	 *
	 * @param id The mosaic id.
	 * @return The removed mosaic entry.
	 */
	public MosaicEntry remove(final MosaicId id) {
		if (!this.contains(id)) {
			throw new IllegalArgumentException(String.format("mosaic definition '%s' not found in cache", id));
		}

		final MosaicEntryHistory history = this.hashMap.get(id);
		if (1 == history.historyDepth()) {
			this.hashMap.remove(id);
		}

		return history.pop();
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

	// region MosaicEntryHistory

	private static class MosaicEntryHistory {
		private final List<MosaicEntry> mosaicEntries = new ArrayList<>();

		public MosaicEntryHistory() {
		}

		public MosaicEntryHistory(final MosaicEntry mosaicEntry) {
			this.push(mosaicEntry);
		}

		public boolean isEmpty() {
			return this.mosaicEntries.isEmpty();
		}

		public void push(final MosaicEntry mosaicEntry) {
			this.mosaicEntries.add(mosaicEntry);
		}

		public MosaicEntry pop() {
			return this.mosaicEntries.remove(this.mosaicEntries.size() - 1);
		}

		public MosaicEntry last() {
			return this.mosaicEntries.get(this.mosaicEntries.size() - 1);
		}

		public int historyDepth() {
			return this.mosaicEntries.size();
		}

		public MosaicEntryHistory copy() {
			final MosaicEntryHistory copy = new MosaicEntryHistory();
			this.mosaicEntries.forEach(me -> copy.mosaicEntries.add(me.copy()));
			return copy;
		}
	}

	// endregion
}
