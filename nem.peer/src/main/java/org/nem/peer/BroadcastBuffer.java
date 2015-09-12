package org.nem.peer;

import org.nem.core.model.BroadcastablePair;
import org.nem.core.node.NisPeerId;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.SerializableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A class that buffers entities that will be broadcast in the future.
 */
public class BroadcastBuffer {
	private final Map<NisPeerId, Collection<SerializableEntity>> map;
	private final Object lock = new Object();

	/**
	 * Creates a new broadcast buffer.
	 */
	public BroadcastBuffer() {
		this(new HashMap<>());
	}

	/**
	 * Creates a new broadcast buffer.
	 * This constructor is used in tests.
	 *
	 * @param map The map to use;
	 */
	public BroadcastBuffer(final Map<NisPeerId, Collection<SerializableEntity>> map) {
		this.map = map;
	}

	/**
	 * Gets the number of different NIS peer ids in the map.
	 *
	 * @return The size();
	 */
	public int size() {
		synchronized(this.lock) {
			return this.map.size();
		}
	}

	/**
	 * Gets the number of entities in the map.
	 *
	 * @return The number of entities.
	 */
	public int deepSize() {
		synchronized(this.lock) {
			return 0 == this.size() ? 0 : this.map.values().stream().map(Collection::size).reduce(Integer::sum).get();
		}
	}

	/**
	 * Adds a new entity to the map. Creates a new collection if needed.
	 *
	 * @param apiId The api id
	 * @param entity The entity.
	 */
	public void add(final NisPeerId apiId, final SerializableEntity entity) {
		synchronized(this.lock) {
			final Collection<SerializableEntity> entities = this.map.getOrDefault(apiId, new ArrayList<>());
			entities.add(entity);
			this.map.put(apiId, entities);
		}
	}

	/**
	 * Gets a collection of all broadcastable pairs and clears the map afterwards.
	 *
	 * @return The collection of broadcastable pairs.
	 */
	public Collection<BroadcastablePair> getAllPairsAndClearMap() {
		synchronized(this.lock) {
			final Collection<BroadcastablePair> pairs = this.map.keySet().stream()
					.map(apiId -> new BroadcastablePair(apiId, new SerializableList<>(this.map.get(apiId))))
					.collect(Collectors.toList());
			this.map.clear();
			return pairs;
		}
	}
}
