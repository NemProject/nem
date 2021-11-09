package org.nem.peer;

import org.nem.core.node.NisPeerId;
import org.nem.core.serialization.SerializableEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Combines a peer network with a broadcast buffer.
 */
public class PeerNetworkBroadcastBuffer {
	private final PeerNetwork network;
	private final BroadcastBuffer buffer;

	/**
	 * Creates a peer network broadcast buffer.
	 *
	 * @param network The network.
	 * @param buffer The buffer.
	 */
	public PeerNetworkBroadcastBuffer(final PeerNetwork network, final BroadcastBuffer buffer) {
		this.network = network;
		this.buffer = buffer;
	}

	/**
	 * Queues an entity to be broadcast later.
	 *
	 * @param broadcastId The type of entity.
	 * @param entity The entity.
	 */
	public void queue(final NisPeerId broadcastId, final SerializableEntity entity) {
		this.buffer.add(broadcastId, entity);
	}

	/**
	 * Broadcasts all queued entities.
	 *
	 * @return The future.
	 */
	public CompletableFuture<Void> broadcastAll() {
		final List<CompletableFuture<?>> futures = this.buffer.getAllPairsAndClearMap().stream()
				.map(p -> this.network.broadcast(p.getApiId(), p.getEntities())).collect(Collectors.toList());
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()]));
	}
}
