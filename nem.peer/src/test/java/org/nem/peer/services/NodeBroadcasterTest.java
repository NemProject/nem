package org.nem.peer.services;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.test.MockSerializableEntity;
import org.nem.core.utils.ExceptionUtils;
import org.nem.peer.connect.PeerConnector;
import org.nem.peer.test.PeerUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NodeBroadcasterTest {

	@Test
	public void broadcastCallsAnnounceAllSpecifiedNodes() {
		// Arrange:
		final TestContext context = new TestContext();
		final SerializableEntity entity = new MockSerializableEntity();

		// Act:
		context.broadcaster.broadcast(context.broadcastNodes, NisPeerId.REST_PUSH_TRANSACTION, entity).join();

		// Assert:
		for (final Node node : context.broadcastNodes) {
			Mockito.verify(context.connector, Mockito.times(1)).announce(node, NisPeerId.REST_PUSH_TRANSACTION, entity);
		}
	}

	@Test
	public void broadcastIsAsync() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.announce(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(CompletableFuture.supplyAsync(() -> {
					ExceptionUtils.propagateVoid(() -> Thread.sleep(300));
					return null;
				}));

		// Act:
		final CompletableFuture<?> future = context.broadcaster.broadcast(context.broadcastNodes, NisPeerId.REST_PUSH_TRANSACTION,
				new MockSerializableEntity());

		// Assert:
		MatcherAssert.assertThat(future.isDone(), IsEqual.equalTo(false));
	}

	// endregion

	private static PeerConnector mockPeerConnector() {
		final PeerConnector connector = Mockito.mock(PeerConnector.class);
		Mockito.when(connector.announce(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenAnswer(i -> CompletableFuture.completedFuture(null));
		return connector;
	}

	private static class TestContext {
		private final List<Node> broadcastNodes = PeerUtils.createNodesWithNames("a", "b", "c");
		private final PeerConnector connector = mockPeerConnector();
		private final NodeBroadcaster broadcaster = new NodeBroadcaster(this.connector);
	}
}
