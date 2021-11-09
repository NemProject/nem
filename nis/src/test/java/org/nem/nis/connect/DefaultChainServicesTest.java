package org.nem.nis.connect;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.FatalPeerException;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.WeakNodeIdentity;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.peer.services.ChainServices;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class DefaultChainServicesTest {

	// region isChainSynchronized

	@Test
	public void isChainSynchronizedReturnsTrueIfLocalChainHasEqualChainScore() {
		// Arrange:
		final TestContext context = new TestContext(30);

		// Act:
		final boolean isSynchronized = context.services.isChainSynchronized(context.nodes.asCollection()).join();

		// Assert:
		MatcherAssert.assertThat(isSynchronized, IsEqual.equalTo(true));
	}

	@Test
	public void isChainSynchronizedReturnsTrueIfLocalChainHasBetterChainScore() {
		// Arrange:
		final TestContext context = new TestContext(31);

		// Act:
		final boolean isSynchronized = context.services.isChainSynchronized(context.nodes.asCollection()).join();

		// Assert:
		MatcherAssert.assertThat(isSynchronized, IsEqual.equalTo(true));
	}

	@Test
	public void isChainSynchronizedReturnsFalseIfLocalChainHasWorseChainScore() {
		// Arrange:
		final TestContext context = new TestContext(29);

		// Act:
		final boolean isSynchronized = context.services.isChainSynchronized(context.nodes.asCollection()).join();

		// Assert:
		MatcherAssert.assertThat(isSynchronized, IsEqual.equalTo(false));
	}

	// endregion

	// region getMaxChainHeightAsync

	@Test
	public void getMaxChainHeightAsyncReturnsMaxHeightOnSuccess() {
		// Assert:
		assertGetMaxChainHeightAsyncReturnsMaxHeightOnSuccess((context, nodes) -> context.services.getMaxChainHeightAsync(nodes));
	}

	private static void assertGetMaxChainHeightAsyncReturnsMaxHeightOnSuccess(
			final BiFunction<TestContext, Collection<Node>, CompletableFuture<BlockHeight>> getMaxChainHeightAsync) {
		// Arrange:
		final TestContext context = new TestContext();
		context.setChainHeightForNode(context.nodes.get(0), createBlockHeightFuture(10));
		context.setChainHeightForNode(context.nodes.get(1), createBlockHeightFuture(30));
		context.setChainHeightForNode(context.nodes.get(2), createBlockHeightFuture(20));

		// Act:
		final BlockHeight maxBlockHeight = getMaxChainHeightAsync.apply(context, context.nodes.asCollection()).join();

		// Assert:
		MatcherAssert.assertThat(maxBlockHeight, IsEqual.equalTo(new BlockHeight(30)));

		context.verifyNumChainHeightRequests(3);
		context.nodes.asCollection().forEach(context::verifySingleChainHeightRequest);
	}

	@Test
	public void getMaxChainHeightAsyncIgnoresFailedNodes() {
		// Assert:
		assertGetMaxChainHeightAsyncIgnoresFailedNodes((context, nodes) -> context.services.getMaxChainHeightAsync(nodes));
	}

	private static void assertGetMaxChainHeightAsyncIgnoresFailedNodes(
			final BiFunction<TestContext, Collection<Node>, CompletableFuture<BlockHeight>> getMaxChainHeightAsync) {
		// Arrange:
		final TestContext context = new TestContext();
		context.setChainHeightForNode(context.nodes.get(0), createExceptionalFuture());
		context.setChainHeightForNode(context.nodes.get(1), createBlockHeightFuture(30));
		context.setChainHeightForNode(context.nodes.get(2), createExceptionalFuture());

		// Act:
		final BlockHeight maxBlockHeight = getMaxChainHeightAsync.apply(context, context.nodes.asCollection()).join();

		// Assert:
		MatcherAssert.assertThat(maxBlockHeight, IsEqual.equalTo(new BlockHeight(30)));

		context.verifyNumChainHeightRequests(3);
		context.nodes.asCollection().forEach(context::verifySingleChainHeightRequest);
	}

	@Test
	public void getMaxChainHeightAsyncReturnsBlockHeightOneIfAllNodesFail() {
		// Assert:
		assertGetMaxChainHeightAsyncReturnsBlockHeightOneIfAllNodesFail((context, nodes) -> context.services.getMaxChainHeightAsync(nodes));
	}

	private static void assertGetMaxChainHeightAsyncReturnsBlockHeightOneIfAllNodesFail(
			final BiFunction<TestContext, Collection<Node>, CompletableFuture<BlockHeight>> getMaxChainHeightAsync) {
		// Arrange:
		final TestContext context = new TestContext();
		context.setChainHeightForNode(context.nodes.get(0), createExceptionalFuture());
		context.setChainHeightForNode(context.nodes.get(1), createExceptionalFuture());
		context.setChainHeightForNode(context.nodes.get(2), createExceptionalFuture());

		// Act:
		final BlockHeight maxBlockHeight = getMaxChainHeightAsync.apply(context, context.nodes.asCollection()).join();

		// Assert:
		MatcherAssert.assertThat(maxBlockHeight, IsEqual.equalTo(BlockHeight.ONE));

		context.verifyNumChainHeightRequests(3);
		context.nodes.asCollection().forEach(context::verifySingleChainHeightRequest);
	}

	// endregion

	// region delegation

	@Test
	public void isChainSynchronizedDelegatesToBlockChainLastBlockChainLayer() {
		// Arrange:
		final TestContext context = new TestContext(29);

		// Act:
		context.services.isChainSynchronized(context.nodes.asCollection()).join();

		// Assert:
		Mockito.verify(context.blockChainLastBlockLayer, Mockito.times(1)).getLastBlockHeight();
	}

	@Test
	public void getMaxChainHeightAsyncDelegatesToConnectorPool() {
		// Arrange:
		final TestContext context = new TestContext(29);

		// Act:
		context.services.getMaxChainHeightAsync(context.nodes.asCollection()).join();

		// Assert:
		Mockito.verify(context.connectorPool, Mockito.times(3)).getSyncConnector(null);
	}

	// endregion

	private static CompletableFuture<BlockHeight> createExceptionalFuture() {
		return CompletableFuture.supplyAsync(() -> {
			throw new FatalPeerException("badness");
		});
	}

	private static CompletableFuture<BlockHeight> createBlockHeightFuture(final long height) {
		return CompletableFuture.completedFuture(new BlockHeight(height));
	}

	private static class TestContext {
		private final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final HttpConnectorPool connectorPool = Mockito.mock(HttpConnectorPool.class);
		private final HttpConnector connector = Mockito.mock(HttpConnector.class);
		private final ChainServices services = new DefaultChainServices(this.blockChainLastBlockLayer, this.connectorPool);
		private final SerializableList<Node> nodes = this.createNodes();

		public TestContext() {
			Mockito.when(this.connectorPool.getPeerConnector(Mockito.any())).thenReturn(this.connector);
			Mockito.when(this.connectorPool.getSyncConnector(Mockito.any())).thenReturn(this.connector);
			Mockito.when(this.connector.getKnownPeers(Mockito.any())).thenReturn(this.createNodesFuture());
		}

		@SuppressWarnings("unchecked")
		public TestContext(final long height) {
			Mockito.when(this.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(new BlockHeight(height));
			Mockito.when(this.connectorPool.getPeerConnector(Mockito.any())).thenReturn(this.connector);
			Mockito.when(this.connectorPool.getSyncConnector(Mockito.any())).thenReturn(this.connector);
			Mockito.when(this.connector.getKnownPeers(Mockito.any())).thenReturn(this.createNodesFuture());
			Mockito.when(this.connector.getChainHeightAsync(Mockito.any(Node.class))).thenReturn(
					CompletableFuture.completedFuture(new BlockHeight(10)), CompletableFuture.completedFuture(new BlockHeight(30)),
					CompletableFuture.completedFuture(new BlockHeight(20)));
		}

		private void setChainHeightForNode(final Node node, final CompletableFuture<BlockHeight> future) {
			Mockito.when(this.connector.getChainHeightAsync(node)).thenReturn(future);
		}

		public Node createNode(final String name) {
			return new Node(new WeakNodeIdentity(name), new NodeEndpoint("http", "10.10.10.12", 1234),
					new NodeMetaData("platform", "FooBar"));
		}

		public SerializableList<Node> createNodes() {
			final SerializableList<Node> nodes = new SerializableList<>(3);
			nodes.add(this.createNode("a"));
			nodes.add(this.createNode("b"));
			nodes.add(this.createNode("c"));
			return nodes;
		}

		private void verifyNumChainHeightRequests(final int numExpectedRequests) {
			Mockito.verify(this.connector, Mockito.times(numExpectedRequests)).getChainHeightAsync(Mockito.any());
		}

		private void verifySingleChainHeightRequest(final Node expectedNode) {
			Mockito.verify(this.connector, Mockito.times(1)).getChainHeightAsync(expectedNode);
		}

		private CompletableFuture<SerializableList<Node>> createNodesFuture() {
			return CompletableFuture.completedFuture(this.nodes);
		}
	}
}
