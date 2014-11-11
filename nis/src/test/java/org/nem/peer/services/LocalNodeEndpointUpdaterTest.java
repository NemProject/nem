package org.nem.peer.services;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.*;
import org.nem.core.node.*;
import org.nem.core.test.NodeUtils;
import org.nem.peer.connect.PeerConnector;
import org.nem.peer.trust.NodeSelector;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LocalNodeEndpointUpdaterTest {

	//region update

	@Test
	public void updateDelegatesToNodeSelectorForNodeSelection() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.updater.update(context.selector).join();

		// Assert:
		Mockito.verify(context.selector, Mockito.only()).selectNode();
	}

	@Test
	public void updateReturnsFalseWhenThereAreNoCommunicationPartners() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.updater.update(context.selector).join();

		// Assert:
		Mockito.verify(context.connector, Mockito.never()).getLocalNodeInfo(Mockito.any(), Mockito.any());
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	@Test
	public void updateDoesNotUpdateEndpointWhenGetLocalInfoFailsWithInactiveException() {
		// Assert:
		assertEndpointIsNotUpdatedWhenGetLocalInfoFailsWithException(new InactivePeerException("inactive"));
	}

	@Test
	public void updateDoesNotUpdateEndpointWhenGetLocalInfoFailsWithFatalException() {
		// Assert:
		assertEndpointIsNotUpdatedWhenGetLocalInfoFailsWithException(new FatalPeerException("fatal"));
	}

	private static void assertEndpointIsNotUpdatedWhenGetLocalInfoFailsWithException(final RuntimeException ex) {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getLocalNodeInfo(Mockito.any(), Mockito.eq(context.localNode.getEndpoint())))
				.thenReturn(CompletableFuture.supplyAsync(() -> { throw ex; }));
		context.makeSelectorReturnRemoteNode();

		// Act:
		final boolean result = context.updater.update(context.selector).join();

		// Assert:
		Mockito.verify(context.connector, Mockito.times(1)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.1"));
	}

	@Test
	public void updateDoesNotUpdateEndpointWhenGetLocalInfoReturnsNull() {
		// Assert:
		assertEndpointIsNotUpdatedByReturnedEndpoint(null, false);
	}

	@Test
	public void updateDoesNotUpdateEndpointWhenEndpointIsUnchanged() {
		// Assert:
		assertEndpointIsNotUpdatedByReturnedEndpoint(NodeEndpoint.fromHost("127.0.0.1"), true);
	}

	private static void assertEndpointIsNotUpdatedByReturnedEndpoint(
			final NodeEndpoint endpoint,
			final boolean expectedResult) {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getLocalNodeInfo(Mockito.any(), Mockito.eq(context.localNode.getEndpoint())))
				.thenReturn(CompletableFuture.completedFuture(endpoint));
		context.makeSelectorReturnRemoteNode();

		// Act:
		final boolean result = context.updater.update(context.selector).join();

		// Assert:
		Mockito.verify(context.connector, Mockito.times(1)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		Assert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.1"));
	}

	@Test
	public void updateUpdatesEndpointWhenEndpointIsChanged() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getLocalNodeInfo(Mockito.any(), Mockito.eq(context.localNode.getEndpoint())))
				.thenReturn(CompletableFuture.completedFuture(NodeEndpoint.fromHost("127.0.0.101")));
		context.makeSelectorReturnRemoteNode();

		// Act:
		final boolean result = context.updater.update(context.selector).join();

		// Assert:
		Mockito.verify(context.connector, Mockito.times(1)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.101"));
	}

	//endregion

	//region updateAny

	@Test
	public void updateAnyUpdatesEndpointWithOneSuccessfulResult() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.runUpdateAnyWithThreeNodes(
				CompletableFuture.completedFuture(NodeEndpoint.fromHost("127.0.0.10")),
				CompletableFuture.completedFuture(NodeEndpoint.fromHost("127.0.0.20")),
				CompletableFuture.completedFuture(NodeEndpoint.fromHost("127.0.0.30")));

		// Assert:
		Mockito.verify(context.connector, Mockito.times(3)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.30"));
	}

	@Test
	public void updateAnySucceedsWhenAtLeastOneNodeIsAbleToUpdateLocalEndpoint() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.runUpdateAnyWithThreeNodes(
				createExceptionalFuture(),
				CompletableFuture.completedFuture(NodeEndpoint.fromHost("127.0.0.20")),
				createExceptionalFuture());

		// Assert:
		Mockito.verify(context.connector, Mockito.times(3)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.20"));
	}

	@Test
	public void updateAnyFailsWhenNoNodesAreAbleToUpdateLocalEndpoint() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.runUpdateAnyWithThreeNodes(
				createExceptionalFuture(),
				createExceptionalFuture(),
				createExceptionalFuture());

		// Assert:
		Mockito.verify(context.connector, Mockito.times(3)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.1"));
	}

	private static CompletableFuture<NodeEndpoint> createExceptionalFuture() {
		return CompletableFuture.supplyAsync(() -> {
			throw new FatalPeerException("badness");
		});
	}

	//endregion

	private static class TestContext {
		private final Node localNode = NodeUtils.createNodeWithName("l");
		private final NodeSelector selector = Mockito.mock(NodeSelector.class);
		private final PeerConnector connector = Mockito.mock(PeerConnector.class);
		private final LocalNodeEndpointUpdater updater = new LocalNodeEndpointUpdater(this.localNode, this.connector);

		public void makeSelectorReturnRemoteNode() {
			Mockito.when(this.selector.selectNode()).thenReturn(NodeUtils.createNodeWithName("p"));
		}

		public boolean runUpdateAnyWithThreeNodes(
				final CompletableFuture<NodeEndpoint> node1Future,
				final CompletableFuture<NodeEndpoint> node2Future,
				final CompletableFuture<NodeEndpoint> node3Future) {
			final List<Node> nodes = Arrays.asList(
					NodeUtils.createNodeWithName("a"),
					NodeUtils.createNodeWithName("b"),
					NodeUtils.createNodeWithName("c"));
			Mockito.when(this.connector.getLocalNodeInfo(Mockito.eq(nodes.get(0)), Mockito.any()))
					.thenReturn(node1Future);
			Mockito.when(this.connector.getLocalNodeInfo(Mockito.eq(nodes.get(1)), Mockito.any()))
					.thenReturn(node2Future);
			Mockito.when(this.connector.getLocalNodeInfo(Mockito.eq(nodes.get(2)), Mockito.any()))
					.thenReturn(node3Future);

			// Act:
			return this.updater.updateAny(nodes).join();
		}
	}
}