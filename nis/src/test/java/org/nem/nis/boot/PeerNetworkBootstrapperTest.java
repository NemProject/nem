package org.nem.nis.boot;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.ExceptionAssert;
import org.nem.core.utils.ExceptionUtils;
import org.nem.peer.*;
import org.nem.peer.services.*;
import org.nem.peer.test.PeerUtils;
import org.nem.peer.trust.NodeSelector;

import java.util.concurrent.CompletableFuture;

public class PeerNetworkBootstrapperTest {

	private static final boolean REQUIRE_ACK = true;
	private static final boolean DO_NOT_REQUIRE_ACK = false;

	@Test
	public void constructorDoesNotBootNetwork() {
		// Act:
		final TestContext context = new TestContext(DO_NOT_REQUIRE_ACK);

		// Assert:
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(true));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(false));
	}

	@Test
	public void networkCanBeBooted() {
		// Arrange:
		final TestContext context = new TestContext(REQUIRE_ACK);

		// Act:
		final PeerNetwork network = context.bootstrapper.boot().join();

		// Assert:
		Assert.assertThat(network, IsNull.notNullValue());
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(false));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(true));
	}

	@Test
	public void networkBootIsAsync() {
		// Arrange:
		final TestContext context = new TestContext(REQUIRE_ACK);
		Mockito.when(context.refresher.refresh(Mockito.any()))
				.thenReturn(CompletableFuture.supplyAsync(() -> {
					ExceptionUtils.propagateVoid(() -> Thread.sleep(300));
					return null;
				}));

		// Act:
		final CompletableFuture<?> future = context.bootstrapper.boot();

		// Assert:
		Assert.assertThat(future.isDone(), IsEqual.equalTo(false));
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(false));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(false));
	}

	@Test
	public void networkBootFailsOnRefreshException() {
		// Arrange:
		final TestContext context = new TestContext(DO_NOT_REQUIRE_ACK);
		Mockito.when(context.refresher.refresh(Mockito.any()))
				.thenReturn(CompletableFuture.supplyAsync(() -> { throw new RuntimeException("runtime"); }));

		// Act:
		ExceptionAssert.assertThrowsCompletionException(
				v -> context.bootstrapper.boot().join(),
				IllegalStateException.class);

		// Assert:
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(true));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(false));
	}

	@Test
	public void networkBootFailsOnUpdateLocalNodeEndpointException() {
		// Arrange:
		final TestContext context = new TestContext(DO_NOT_REQUIRE_ACK);
		Mockito.when(context.updater.update(Mockito.any()))
				.thenReturn(CompletableFuture.supplyAsync(() -> { throw new RuntimeException("runtime"); }));

		// Act:
		ExceptionAssert.assertThrowsCompletionException(
				v -> context.bootstrapper.boot().join(),
				IllegalStateException.class);

		// Assert:
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(true));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(false));
	}

	@Test
	public void networkBootFailsOnUpdateLocalNodeEndpointFailureWhenAckIsRequired() {
		// Arrange:
		final TestContext context = new TestContext(REQUIRE_ACK);
		Mockito.when(context.updater.update(Mockito.any()))
				.thenReturn(CompletableFuture.completedFuture(false));

		// Act:
		ExceptionAssert.assertThrowsCompletionException(
				v -> context.bootstrapper.boot().join(),
				IllegalStateException.class);

		// Assert:
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(true));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(false));
	}

	@Test
	public void networkBootSucceedsOnUpdateLocalNodeEndpointFailureWhenAckIsNotRequired() {
		// Arrange:
		final TestContext context = new TestContext(DO_NOT_REQUIRE_ACK);
		Mockito.when(context.updater.update(Mockito.any()))
				.thenReturn(CompletableFuture.completedFuture(false));

		// Act:
		context.bootstrapper.boot().join();

		// Assert:
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(false));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(true));
	}

	@Test
	public void networkCannotBeBootedMoreThanOnce() {
		// Arrange:
		final TestContext context = new TestContext(DO_NOT_REQUIRE_ACK);

		// Act:
		context.bootstrapper.boot().join();
		ExceptionAssert.assertThrows(
				v -> context.bootstrapper.boot().join(),
				IllegalStateException.class);
	}

	@Test
	public void bootCanBeRetriedIfInitialBootFails() {
		// Arrange:
		final TestContext context = new TestContext(DO_NOT_REQUIRE_ACK);

		// Act: first boot should fail
		Mockito.when(context.updater.update(Mockito.any()))
				.thenReturn(CompletableFuture.supplyAsync(() -> { throw new RuntimeException("runtime"); }));
		ExceptionAssert.assertThrowsCompletionException(
				v -> context.bootstrapper.boot().join(),
				IllegalStateException.class);

		// Act: second boot should succeed
		Mockito.when(context.updater.update(Mockito.any()))
				.thenReturn(CompletableFuture.completedFuture(true));
		final PeerNetwork network = context.bootstrapper.boot().join();

		// Assert:
		Assert.assertThat(network, IsNull.notNullValue());
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(false));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(true));
	}

	private static class TestContext {
		private final PeerNetworkState state = Mockito.mock(PeerNetworkState.class);
		private final PeerNetworkServicesFactory servicesFactory = Mockito.mock(PeerNetworkServicesFactory.class);
		private final NodeSelectorFactory selectorFactory = Mockito.mock(NodeSelectorFactory.class);
		private final NodeSelectorFactory importanceAwareSelectorFactory = Mockito.mock(NodeSelectorFactory.class);
		private final NodeRefresher refresher = Mockito.mock(NodeRefresher.class);
		private final PeerNetworkBootstrapper bootstrapper;
		private final LocalNodeEndpointUpdater updater = Mockito.mock(LocalNodeEndpointUpdater.class);

		public TestContext(final boolean requirePeerAck) {
			final NodeSelector selector = Mockito.mock(NodeSelector.class);
			Mockito.when(selector.selectNodes()).thenReturn(PeerUtils.createNodesWithNames("a", "b", "c"));
			Mockito.when(selector.selectNode()).thenReturn(PeerUtils.createNodeWithName("d"));
			Mockito.when(this.selectorFactory.createNodeSelector()).thenReturn(selector);

			Mockito.when(this.refresher.refresh(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
			Mockito.when(this.servicesFactory.createNodeRefresher()).thenReturn(this.refresher);

			Mockito.when(this.updater.update(Mockito.any())).thenReturn(CompletableFuture.completedFuture(true));
			Mockito.when(this.servicesFactory.createLocalNodeEndpointUpdater()).thenReturn(this.updater);

			this.bootstrapper = new PeerNetworkBootstrapper(
					this.state,
					this.servicesFactory,
					this.selectorFactory,
					this.importanceAwareSelectorFactory,
					requirePeerAck);
		}
	}
}