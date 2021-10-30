package org.nem.nis.controller;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.NemStatus;
import org.nem.core.model.ncc.NemRequestResult;
import org.nem.core.node.*;
import org.nem.core.test.NodeUtils;
import org.nem.deploy.CommonStarter;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.peer.PeerNetwork;

public class LocalControllerTest {

	// region shutdown

	@Test
	public void shutdownDelegatesToCommonStarter() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.controller.shutdown();

		// Assert:
		Mockito.verify(context.starter, Mockito.only()).stopServerAsync();
	}

	// endregion

	// region heartbeat

	@Test
	public void heartbeatReturnsCorrectResult() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final NemRequestResult result = context.controller.heartbeat();

		// Assert:
		MatcherAssert.assertThat(result.getCode(), IsEqual.equalTo(NemRequestResult.CODE_SUCCESS));
		MatcherAssert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_HEARTBEAT));
		MatcherAssert.assertThat(result.getMessage(), IsEqual.equalTo("ok"));
	}

	// endregion

	// region status

	@Test
	public void statusReturnsStatusRunningWhenNetworkIsNotBootedAndNotBooting() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(false);
		Mockito.when(context.host.isNetworkBooting()).thenReturn(false);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.RUNNING);
	}

	@Test
	public void statusReturnsStatusBootingWhenNetworkIsNotBootedButBooting() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(false);
		Mockito.when(context.host.isNetworkBooting()).thenReturn(true);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.BOOTING);
	}

	@Test
	public void statusReturnsStatusBootedWhenNetworkIsBootedButNotSynchronized() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(true);
		Mockito.when(context.network.isChainSynchronized()).thenReturn(false);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.BOOTED);
	}

	@Test
	public void statusReturnsStatusSynchronizedWhenNetworkIsBootedAndSynchronized() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(true);
		Mockito.when(context.network.isChainSynchronized()).thenReturn(true);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.SYNCHRONIZED);
	}

	@Test
	public void statusReturnsStatusNoRemoteNisWhenNetworkIsBootedAndNoRemoteNisIsAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(true);
		Mockito.when(context.network.getNodes()).thenReturn(new NodeCollection());

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.NO_REMOTE_NIS_AVAILABLE);
	}

	@Test
	public void statusReturnsStatusLoadingWhenLastBlockIsNotAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.lastBlockLayer.isLoading()).thenReturn(true);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.LOADING);
	}

	private static void assertStatus(final NemRequestResult result, final NemStatus expectedStatus) {
		// Assert:
		MatcherAssert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_STATUS));
		MatcherAssert.assertThat(result.getCode(), IsEqual.equalTo(expectedStatus.getValue()));
		MatcherAssert.assertThat(result.getMessage(), IsEqual.equalTo("status"));
	}

	// endregion

	private class TestContext {
		private final NisPeerNetworkHost host = Mockito.mock(NisPeerNetworkHost.class);
		private final PeerNetwork network = Mockito.mock(PeerNetwork.class);
		private final CommonStarter starter = Mockito.mock(CommonStarter.class);
		private final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final LocalController controller;

		private TestContext() {
			this.controller = new LocalController(this.host, this.starter, this.lastBlockLayer);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			final NodeCollection nodes = new NodeCollection();
			nodes.update(NodeUtils.createNodeWithName("a"), NodeStatus.ACTIVE);
			Mockito.when(this.network.getNodes()).thenReturn(nodes);

			Mockito.when(this.lastBlockLayer.isLoading()).thenReturn(false);
		}
	}
}
