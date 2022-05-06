package org.nem.peer;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.NisPeerId;
import org.nem.core.serialization.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PeerNetworkBroadcastBufferTest {

	@Test
	public void queueDelegatesToBroadcastBuffer() {
		// Arrange:
		final PeerNetwork network = Mockito.mock(PeerNetwork.class);
		final BroadcastBuffer buffer = Mockito.mock(BroadcastBuffer.class);
		final PeerNetworkBroadcastBuffer networkBroadcastBuffer = new PeerNetworkBroadcastBuffer(network, buffer);

		// Act:
		networkBroadcastBuffer.queue(NisPeerId.REST_PUSH_BLOCK, new BlockHeight(787));

		// Assert:
		Mockito.verify(network, Mockito.never()).broadcast(Mockito.any(), Mockito.any());
		Mockito.verify(buffer, Mockito.only()).add(NisPeerId.REST_PUSH_BLOCK, new BlockHeight(787));
	}

	@Test
	public void broadcastAllDelegatesToNetwork() {
		// Arrange:
		final PeerNetwork network = Mockito.mock(PeerNetwork.class);
		final BroadcastBuffer buffer = Mockito.mock(BroadcastBuffer.class);
		final PeerNetworkBroadcastBuffer networkBroadcastBuffer = new PeerNetworkBroadcastBuffer(network, buffer);

		final Collection<NisPeerIdAndEntityListPair> pairs = Arrays.asList(
				new NisPeerIdAndEntityListPair(NisPeerId.REST_BLOCK_AT, createList(new BlockHeight(123), new BlockHeight(234))),
				new NisPeerIdAndEntityListPair(NisPeerId.REST_CHAIN_HASHES_FROM, createList(new BlockHeight(345))));
		Mockito.when(buffer.getAllPairsAndClearMap()).thenReturn(pairs);

		Mockito.when(network.broadcast(Mockito.any(), Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));

		// Act:
		networkBroadcastBuffer.broadcastAll().join();

		// Assert:
		Mockito.verify(buffer, Mockito.only()).getAllPairsAndClearMap();
		Mockito.verify(network, Mockito.times(2)).broadcast(Mockito.any(), Mockito.any());
		for (final NisPeerIdAndEntityListPair pair : pairs) {
			Mockito.verify(network, Mockito.times(1)).broadcast(pair.getApiId(), pair.getEntities());
		}
	}

	private static SerializableList<SerializableEntity> createList(final SerializableEntity... entities) {
		final SerializableList<SerializableEntity> list = new SerializableList<>(entities.length);
		Arrays.stream(entities).forEach(list::add);
		return list;
	}
}
