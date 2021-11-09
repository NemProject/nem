package org.nem.nis;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;
import org.nem.peer.*;
import org.nem.peer.connect.SyncConnectorPool;

public class CountingBlockSynchronizerTest {

	@Test
	public void initialSyncCountsForNodesAreZero() {
		// Arrange:
		final CountingBlockSynchronizer synchronizer = new CountingBlockSynchronizer(Mockito.mock(BlockSynchronizer.class));

		// Assert:
		MatcherAssert.assertThat(synchronizer.getSyncAttempts(NodeUtils.createNodeWithName("10.0.0.1")), IsEqual.equalTo(0));
		MatcherAssert.assertThat(synchronizer.getSyncAttempts(NodeUtils.createNodeWithName("10.0.0.2")), IsEqual.equalTo(0));
		MatcherAssert.assertThat(synchronizer.getSyncAttempts(NodeUtils.createNodeWithName("10.0.0.3")), IsEqual.equalTo(0));
	}

	@Test
	public void synchronizeNodeDelegatesToWrappedSynchronizer() {
		// Arrange:
		final SyncConnectorPool connectorPool = Mockito.mock(SyncConnectorPool.class);
		final Node node = NodeUtils.createNodeWithName("10.0.0.1");
		final BlockSynchronizer innerSynchronizer = Mockito.mock(BlockSynchronizer.class);
		Mockito.when(innerSynchronizer.synchronizeNode(connectorPool, node)).thenReturn(NodeInteractionResult.FAILURE);

		final CountingBlockSynchronizer synchronizer = new CountingBlockSynchronizer(innerSynchronizer);

		// Act:
		final NodeInteractionResult result = synchronizer.synchronizeNode(connectorPool, node);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(NodeInteractionResult.FAILURE));
		Mockito.verify(innerSynchronizer, Mockito.times(1)).synchronizeNode(connectorPool, node);
	}

	@Test
	public void synchronizeNodeIncrementsNodeSyncAttempts() {
		// Arrange:
		final CountingBlockSynchronizer synchronizer = new CountingBlockSynchronizer(Mockito.mock(BlockSynchronizer.class));

		// Act:
		synchronizer.synchronizeNode(null, NodeUtils.createNodeWithName("10.0.0.3"));
		synchronizer.synchronizeNode(null, NodeUtils.createNodeWithName("10.0.0.1"));
		synchronizer.synchronizeNode(null, NodeUtils.createNodeWithName("10.0.0.3"));

		// Assert:
		MatcherAssert.assertThat(synchronizer.getSyncAttempts(NodeUtils.createNodeWithName("10.0.0.1")), IsEqual.equalTo(1));
		MatcherAssert.assertThat(synchronizer.getSyncAttempts(NodeUtils.createNodeWithName("10.0.0.2")), IsEqual.equalTo(0));
		MatcherAssert.assertThat(synchronizer.getSyncAttempts(NodeUtils.createNodeWithName("10.0.0.3")), IsEqual.equalTo(2));
	}
}
