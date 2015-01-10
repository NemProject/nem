package org.nem.nis.boot;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.nem.core.async.SleepFuture;
import org.nem.core.test.*;
import org.nem.deploy.IpDetectionMode;
import org.nem.peer.*;
import org.nem.peer.services.*;
import org.nem.peer.test.PeerUtils;
import org.nem.peer.trust.NodeSelector;

import java.util.concurrent.CompletableFuture;

public class PeerNetworkBootstrapperTest {
	private static final IpDetectionMode REQUIRE_ACK = IpDetectionMode.AutoRequired;
	private static final IpDetectionMode DO_NOT_REQUIRE_ACK = IpDetectionMode.AutoOptional;

	@Test
	public void constructorDoesNotBootNetwork() {
		// Act:
		final TestContext context = new TestContext(DO_NOT_REQUIRE_ACK);

		// Assert:
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(true));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(false));
		context.verifyBootCalls(Mockito.never());
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
		context.verifyBootCalls(Mockito.only());
	}

	@Test
	public void networkBootIsAsync() {
		// Arrange:
		final TestContext context = new TestContext(REQUIRE_ACK);
		context.setUpdaterResult(SleepFuture.create(300));

		// Act:
		final CompletableFuture<?> future = context.bootstrapper.boot();

		// Assert:
		Assert.assertThat(future.isDone(), IsEqual.equalTo(false));
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(false));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(false));
		context.verifyBootCalls(Mockito.only());
	}

	@Test
	public void networkBootFailsOnUpdateLocalNodeEndpointException() {
		// Arrange:
		final TestContext context = new TestContext(DO_NOT_REQUIRE_ACK);
		context.setUpdaterResult(CompletableFuture.supplyAsync(() -> { throw new RuntimeException("runtime"); }));

		// Act:
		ExceptionAssert.assertThrowsCompletionException(
				v -> context.bootstrapper.boot().join(),
				IllegalStateException.class);

		// Assert:
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(true));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(false));
		context.verifyBootCalls(Mockito.only());
	}

	@Test
	public void networkBootFailsOnUpdateLocalNodeEndpointFailureWhenAckIsRequired() {
		// Arrange:
		final TestContext context = new TestContext(REQUIRE_ACK);
		context.setUpdaterResult(CompletableFuture.completedFuture(false));

		// Act:
		ExceptionAssert.assertThrowsCompletionException(
				v -> context.bootstrapper.boot().join(),
				IllegalStateException.class);

		// Assert:
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(true));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(false));
		context.verifyBootCalls(Mockito.only());
	}

	@Test
	public void networkBootSucceedsOnUpdateLocalNodeEndpointFailureWhenAckIsNotRequired() {
		// Arrange:
		final TestContext context = new TestContext(DO_NOT_REQUIRE_ACK);
		context.setUpdaterResult(CompletableFuture.completedFuture(false));

		// Act:
		context.bootstrapper.boot().join();

		// Assert:
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(false));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(true));
		context.verifyBootCalls(Mockito.only());
	}

	@Test
	public void networkBootBypassesAutoIpDetectionWhenIpDetectionModeIsDisabled() {
		// Arrange:
		final TestContext context = new TestContext(IpDetectionMode.Disabled);

		// Act:
		context.bootstrapper.boot().join();

		// Assert:
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(false));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(true));
		context.verifyBootCalls(Mockito.never());
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

		//Assert:
		context.verifyBootCalls(Mockito.only());
	}

	@Test
	public void bootCanBeRetriedIfInitialBootFails() {
		// Arrange:
		final TestContext context = new TestContext(DO_NOT_REQUIRE_ACK);

		// Act: first boot should fail
		context.setUpdaterResult(CompletableFuture.supplyAsync(() -> { throw new RuntimeException("runtime"); }));
		ExceptionAssert.assertThrowsCompletionException(
				v -> context.bootstrapper.boot().join(),
				IllegalStateException.class);

		// Act: second boot should succeed
		context.setUpdaterResult(CompletableFuture.completedFuture(true));
		final PeerNetwork network = context.bootstrapper.boot().join();

		// Assert:
		Assert.assertThat(network, IsNull.notNullValue());
		Assert.assertThat(context.bootstrapper.canBoot(), IsEqual.equalTo(false));
		Assert.assertThat(context.bootstrapper.isBooted(), IsEqual.equalTo(true));
		context.verifyBootCalls(Mockito.times(2));
	}

	private static class TestContext {
		private final PeerNetworkState state = Mockito.mock(PeerNetworkState.class);
		private final PeerNetworkServicesFactory servicesFactory = Mockito.mock(PeerNetworkServicesFactory.class);
		private final PeerNetworkNodeSelectorFactory selectorFactory = Mockito.mock(PeerNetworkNodeSelectorFactory.class);
		private final PeerNetworkBootstrapper bootstrapper;
		private final LocalNodeEndpointUpdater updater = Mockito.mock(LocalNodeEndpointUpdater.class);

		public TestContext(final IpDetectionMode ipDetectionMode) {
			final NodeSelector selector = Mockito.mock(NodeSelector.class);
			Mockito.when(selector.selectNodes()).thenReturn(PeerUtils.createNodesWithNames("a", "b", "c"));
			Mockito.when(selector.selectNode()).thenReturn(NodeUtils.createNodeWithName("d"));
			Mockito.when(this.selectorFactory.createUpdateNodeSelector()).thenReturn(selector);

			Mockito.when(this.updater.updateAny(Mockito.any())).thenReturn(CompletableFuture.completedFuture(true));
			Mockito.when(this.servicesFactory.createLocalNodeEndpointUpdater()).thenReturn(this.updater);

			this.bootstrapper = new PeerNetworkBootstrapper(
					this.state,
					this.servicesFactory,
					this.selectorFactory,
					ipDetectionMode);
		}

		public void setUpdaterResult(final CompletableFuture<Boolean> future) {
			Mockito.when(this.updater.updateAny(Mockito.any()))
					.thenReturn(future);
		}

		private void verifyBootCalls(final VerificationMode mode) {
			Mockito.verify(this.updater, mode).updateAny(Mockito.any());
		}
	}
}