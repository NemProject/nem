package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.BlockChain;
import org.nem.peer.connect.*;
import org.nem.peer.test.WeakNodeIdentity;

import java.util.concurrent.CompletableFuture;

public class ChainServicesTest {

	@Test
	public void isChainSynchronizedReturnsTrueIfLocalChainHasEqualChainScore() {
		// Arrange:
		final TestContext context = new TestContext(30);
		final Node node = context.createNode("test");

		// Assert:
		Assert.assertThat(context.services.isChainSynchronized(node), IsEqual.equalTo(true));
	}

	@Test
	public void isChainSynchronizedReturnsTrueIfLocalChainHasBetterChainScore() {
		// Arrange:
		final TestContext context = new TestContext(31);
		final Node node = context.createNode("test");

		// Assert:
		Assert.assertThat(context.services.isChainSynchronized(node), IsEqual.equalTo(true));
	}

	@Test
	public void isChainSynchronizedReturnsFalseIfLocalChainHasWorseChainScore() {
		// Arrange:
		final TestContext context = new TestContext(29);
		final Node node = context.createNode("test");

		// Assert:
		Assert.assertThat(context.services.isChainSynchronized(node), IsEqual.equalTo(false));
	}

	private class TestContext {
		private final BlockChain blockChain = Mockito.mock(BlockChain.class);
		private final HttpConnectorPool connectorPool = Mockito.mock(HttpConnectorPool.class);
		private final HttpConnector connector = Mockito.mock(HttpConnector.class);
		private final ChainServices services = new ChainServices(this.blockChain, this.connectorPool);

		@SuppressWarnings("unchecked")
		public TestContext(final long score) {
			Mockito.when(this.blockChain.getScore()).thenReturn(new BlockChainScore(score));
			Mockito.when(this.connectorPool.getPeerConnector(Mockito.any())).thenReturn(this.connector);
			Mockito.when(this.connectorPool.getSyncConnector(Mockito.any())).thenReturn(this.connector);
			Mockito.when(this.connector.getKnownPeers(Mockito.any())).thenReturn(createNodes());
			Mockito.when(this.connector.getChainScoreAsync(Mockito.any(Node.class))).thenReturn(
					CompletableFuture.completedFuture(new BlockChainScore(10)),
					CompletableFuture.completedFuture(new BlockChainScore(30)),
					CompletableFuture.completedFuture(new BlockChainScore(20)));
		}

		public Node createNode(final String name) {
			return new Node(
					new WeakNodeIdentity(name),
					new NodeEndpoint("http", "10.10.10.12", 1234),
					new NodeMetaData("platform", "FooBar", NodeVersion.ZERO));
		}

		private CompletableFuture<SerializableList<Node>> createNodes() {
			final SerializableList<Node> nodes = new SerializableList<>(3);
			nodes.add(createNode("a"));
			nodes.add(createNode("b"));
			nodes.add(createNode("c"));
			return CompletableFuture.completedFuture(nodes);
		}
	}
}