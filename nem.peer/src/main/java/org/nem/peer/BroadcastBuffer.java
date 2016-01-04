package org.nem.peer;

import org.nem.core.node.NisPeerId;
import org.nem.core.serialization.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A class that buffers entities that will be broadcast in the future.
 */
public class BroadcastBuffer {
	private final Map<NisPeerId, Collection<SerializableEntity>> map = new HashMap<>();
	private final Object lock = new Object();

	/**
	 * Gets the number of different NIS peer ids in the map.
	 *
	 * @return The size();
	 */
	public int size() {
		synchronized (this.lock) {
			return this.map.size();
		}
	}

	/**
	 * Gets the number of entities in all maps.
	 *
	 * @return The number of entities.
	 */
	public int deepSize() {
		synchronized (this.lock) {
			return this.map.values().stream().map(Collection::size).reduce(0, Integer::sum);
		}
	}

	/**
	 * Adds a new entity to the map. Creates a new collection if needed.
	 *
	 * @param apiId The api id
	 * @param entity The entity.
	 */
	public void add(final NisPeerId apiId, final SerializableEntity entity) {
		synchronized (this.lock) {
			Collection<SerializableEntity> entities = this.map.getOrDefault(apiId, null);
			if (null == entities) {
				entities = new ArrayList<>();
			}

			entities.add(entity);
			this.map.put(apiId, entities);
		}
	}

	/**
	 * Gets a collection of all broadcastable pairs.
	 *
	 * @return The collection of broadcastable pairs.
	 */
	public Collection<NisPeerIdAndEntityListPair> getAllPairs() {
		synchronized (this.lock) {
			return this.map.keySet().stream()
					.map(apiId -> new NisPeerIdAndEntityListPair(apiId, new SerializableList<>(this.map.get(apiId))))
					.collect(Collectors.toList());
		}
	}

	/**
	 * Gets a collection of all broadcastable pairs and clears the map afterwards.
	 *
	 * @return The collection of broadcastable pairs.
	 */
	public Collection<NisPeerIdAndEntityListPair> getAllPairsAndClearMap() {
		synchronized (this.lock) {
			final Collection<NisPeerIdAndEntityListPair> pairs = this.getAllPairs();
			this.map.clear();
			return pairs;
		}
	}
}
