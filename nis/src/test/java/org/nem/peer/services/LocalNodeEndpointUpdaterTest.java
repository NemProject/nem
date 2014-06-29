package org.nem.peer.services;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.connect.*;
import org.nem.peer.connect.*;
import org.nem.peer.node.*;
import org.nem.peer.test.*;
import org.nem.peer.trust.*;

import java.util.concurrent.CompletableFuture;

public class LocalNodeEndpointUpdaterTest {

	@Test
	public void updateDelegatesToNodeSelectorForNodeSelection() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.updater.update(context.selector).join();

		// Assert:
		Mockito.verify(context.selector, Mockito.times(1)).selectNode();
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
		assertEndpointIsNotUpdatedByReturnedEndpoint(null);
	}

	@Test
	public void updateDoesNotUpdateEndpointWhenEndpointIsUnchanged() {
		// Assert:
		assertEndpointIsNotUpdatedByReturnedEndpoint(NodeEndpoint.fromHost("127.0.0.1"));
	}

	private static void assertEndpointIsNotUpdatedByReturnedEndpoint(final NodeEndpoint endpoint) {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.connector.getLocalNodeInfo(Mockito.any(), Mockito.eq(context.localNode.getEndpoint())))
				.thenReturn(CompletableFuture.completedFuture(endpoint));
		context.makeSelectorReturnRemoteNode();

		// Act:
		final boolean result = context.updater.update(context.selector).join();

		// Assert:
		Mockito.verify(context.connector, Mockito.times(1)).getLocalNodeInfo(Mockito.any(), Mockito.any());
		Assert.assertThat(result, IsEqual.equalTo(false));
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

	private static class TestContext {
		private final Node localNode = PeerUtils.createNodeWithName("l");
		private final NodeSelector selector = Mockito.mock(NodeSelector.class);
		private final PeerConnector connector = Mockito.mock(PeerConnector.class);
		private final LocalNodeEndpointUpdater updater = new LocalNodeEndpointUpdater(this.localNode, this.connector);

		public void makeSelectorReturnRemoteNode() {
			Mockito.when(this.selector.selectNode()).thenReturn(PeerUtils.createNodeWithName("p"));
		}
	}
}