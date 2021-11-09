package org.nem.peer.services;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.*;
import org.nem.core.node.*;
import org.nem.core.test.NodeUtils;
import org.nem.peer.connect.PeerConnector;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LocalNodeEndpointUpdaterTest {

	// region update

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
				.thenReturn(CompletableFuture.supplyAsync(() -> {
					throw ex;
				}));

		// Act:
		final boolean result = context.updater.update(context.remoteNode).join();

		// Assert:
		Mockito.verify(context.connector, Mockito.times(1)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		MatcherAssert.assertThat(result, IsEqual.equalTo(false));
		MatcherAssert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.1"));
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

	private static void assertEndpointIsNotUpdatedByReturnedEndpoint(final NodeEndpoint endpoint, final boolean expectedResult) {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getLocalNodeInfo(Mockito.any(), Mockito.eq(context.localNode.getEndpoint())))
				.thenReturn(CompletableFuture.completedFuture(endpoint));

		// Act:
		final boolean result = context.updater.update(context.remoteNode).join();

		// Assert:
		Mockito.verify(context.connector, Mockito.times(1)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
		MatcherAssert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.1"));
	}

	@Test
	public void updateUpdatesEndpointWhenEndpointIsChanged() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getLocalNodeInfo(Mockito.any(), Mockito.eq(context.localNode.getEndpoint())))
				.thenReturn(createEndpointFuture("127.0.0.101"));

		// Act:
		final boolean result = context.updater.update(context.remoteNode).join();

		// Assert:
		Mockito.verify(context.connector, Mockito.times(1)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
		MatcherAssert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.101"));
	}

	// endregion

	// region updateAny

	@Test
	public void updateAnyUpdatesEndpointWithOneSuccessfulResult() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.runUpdateAny(createEndpointFuture("127.0.0.10"), createEndpointFuture("127.0.0.20"),
				createEndpointFuture("127.0.0.30"));

		// Assert:
		Mockito.verify(context.connector, Mockito.times(3)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
		MatcherAssert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.10"));
	}

	@Test
	public void updateAnySucceedsWhenAtLeastOneNodeIsAbleToUpdateLocalEndpoint() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.runUpdateAny(createExceptionalFuture(), createEndpointFuture("127.0.0.20"),
				createExceptionalFuture());

		// Assert:
		Mockito.verify(context.connector, Mockito.times(3)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
		MatcherAssert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.20"));
	}

	@Test
	public void updateAnyFailsWhenNoNodesAreAbleToUpdateLocalEndpoint() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.runUpdateAny(createExceptionalFuture(), createExceptionalFuture(), createExceptionalFuture());

		// Assert:
		Mockito.verify(context.connector, Mockito.times(3)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		MatcherAssert.assertThat(result, IsEqual.equalTo(false));
		MatcherAssert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.1"));
	}

	// endregion

	// region updatePlurality

	@Test
	public void updatePluralityPicksAnyEndpointWhenAllEndpointsHaveSameAgreement() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.runUpdatePlurality(createEndpointFuture("127.0.0.10"), createEndpointFuture("127.0.0.20"),
				createEndpointFuture("127.0.0.30"));

		// Assert:
		Mockito.verify(context.connector, Mockito.times(3)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
		MatcherAssert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.30"));
	}

	@Test
	public void updatePluralityPicksEndpointWithHighestAgreement() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.runUpdatePlurality(createEndpointFuture("127.0.0.30"), createEndpointFuture("127.0.0.20"),
				createEndpointFuture("127.0.0.10"), createEndpointFuture("127.0.0.10"), createEndpointFuture("127.0.0.20"),
				createEndpointFuture("127.0.0.10"), createEndpointFuture("127.0.0.40"));

		// Assert:
		Mockito.verify(context.connector, Mockito.times(7)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
		MatcherAssert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.10"));
	}

	@Test
	public void updatePluralityPicksEndpointWithHighestAgreementWhenThereAreSomeFailures() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.runUpdatePlurality(createEndpointFuture("127.0.0.30"), createExceptionalFuture(),
				createEndpointFuture("127.0.0.10"), createEndpointFuture("127.0.0.10"), createExceptionalFuture(),
				createExceptionalFuture());

		// Assert:
		Mockito.verify(context.connector, Mockito.times(6)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
		MatcherAssert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.10"));
	}

	@Test
	public void updatePluralityFailsWhenNoNodesAreAbleToUpdateLocalEndpoint() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.runUpdatePlurality(createExceptionalFuture(), createExceptionalFuture(), createExceptionalFuture());

		// Assert:
		Mockito.verify(context.connector, Mockito.times(3)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		MatcherAssert.assertThat(result, IsEqual.equalTo(false));
		MatcherAssert.assertThat(context.localNode.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("127.0.0.1"));
	}

	// endregion

	private static CompletableFuture<NodeEndpoint> createExceptionalFuture() {
		return CompletableFuture.supplyAsync(() -> {
			throw new FatalPeerException("badness");
		});
	}

	private static CompletableFuture<NodeEndpoint> createEndpointFuture(final String host) {
		return CompletableFuture.completedFuture(NodeEndpoint.fromHost(host));
	}

	@SuppressWarnings("varargs")
	private static class TestContext {
		private final Node localNode = NodeUtils.createNodeWithName("l");
		private final Node remoteNode = NodeUtils.createNodeWithName("p");
		private final PeerConnector connector = Mockito.mock(PeerConnector.class);
		private final LocalNodeEndpointUpdater updater = new LocalNodeEndpointUpdater(this.localNode, this.connector);

		@SafeVarargs
		public final boolean runUpdateAny(final CompletableFuture<NodeEndpoint>... futures) {
			final List<Node> nodes = this.createNodes(Arrays.asList(futures));

			// Act:
			return this.updater.updateAny(nodes).join();
		}

		@SafeVarargs
		public final boolean runUpdatePlurality(final CompletableFuture<NodeEndpoint>... futures) {
			final List<Node> nodes = this.createNodes(Arrays.asList(futures));

			// Act:
			return this.updater.updatePlurality(nodes).join();
		}

		private List<Node> createNodes(final List<CompletableFuture<NodeEndpoint>> nodeFutures) {
			final List<Node> nodes = new ArrayList<>(nodeFutures.size());
			int i = 0;
			for (final CompletableFuture<NodeEndpoint> future : nodeFutures) {
				nodes.add(NodeUtils.createNodeWithName("n" + i));
				Mockito.when(this.connector.getLocalNodeInfo(Mockito.eq(nodes.get(i)), Mockito.any())).thenReturn(future);
				++i;
			}

			return nodes;
		}
	}
}
