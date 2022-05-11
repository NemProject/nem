package org.nem.peer.services;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;
import org.nem.peer.*;
import org.nem.peer.connect.SyncConnectorPool;
import org.nem.peer.trust.NodeSelector;

public class NodeSynchronizerTest {

	@Test
	public void synchronizeDelegatesToNodeSelectorForNodeSelection() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.synchronizer.synchronize(context.selector);

		// Assert:
		Mockito.verify(context.selector, Mockito.times(1)).selectNode();
	}

	@Test
	public void synchronizeReturnsFalseWhenThereAreNoCommunicationPartners() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.synchronizer.synchronize(context.selector);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(false));
	}

	@Test
	public void synchronizeDelegatesToBlockSynchronizer() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node remoteNode = context.makeSelectorReturnRemoteNode();

		// Act:
		final boolean result = context.synchronizer.synchronize(context.selector);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
		Mockito.verify(context.blockSynchronizer, Mockito.times(1)).synchronizeNode(context.syncConnectorPool, remoteNode);
	}

	@Test
	public void synchronizeDelegatesToStateToUpdatesExperience() {
		// Assert:
		for (final NodeInteractionResult interactionResult : NodeInteractionResult.values()) {
			assertExperienceUpdated(interactionResult);
		}
	}

	private static void assertExperienceUpdated(final NodeInteractionResult interactionResult) {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.blockSynchronizer.synchronizeNode(Mockito.any(), Mockito.any())).thenReturn(interactionResult);
		final Node remoteNode = context.makeSelectorReturnRemoteNode();

		// Act:
		final boolean result = context.synchronizer.synchronize(context.selector);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
		Mockito.verify(context.state, Mockito.times(1)).updateExperience(remoteNode, interactionResult);
	}

	private static class TestContext {
		private final SyncConnectorPool syncConnectorPool = Mockito.mock(SyncConnectorPool.class);
		private final BlockSynchronizer blockSynchronizer = Mockito.mock(BlockSynchronizer.class);
		private final PeerNetworkState state = Mockito.mock(PeerNetworkState.class);
		private final NodeSelector selector = Mockito.mock(NodeSelector.class);
		private final NodeSynchronizer synchronizer = new NodeSynchronizer(this.syncConnectorPool, this.blockSynchronizer, this.state);

		public Node makeSelectorReturnRemoteNode() {
			final Node remoteNode = NodeUtils.createNodeWithName("p");
			Mockito.when(this.selector.selectNode()).thenReturn(remoteNode);
			return remoteNode;
		}
	}
}
