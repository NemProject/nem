package org.nem.nis.boot;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.crypto.KeyPair;
import org.nem.core.node.*;
import org.nem.core.time.*;
import org.nem.nis.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.cache.*;
import org.nem.nis.connect.HttpConnectorPool;
import org.nem.nis.harvesting.HarvestingTask;
import org.nem.nis.test.NisUtils;
import org.nem.peer.connect.CommunicationMode;
import org.nem.peer.node.NodeCompatibilityChecker;
import org.nem.peer.services.ChainServices;
import org.nem.specific.deploy.NisConfiguration;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NisPeerNetworkHostTest {

	@Test
	public void defaultHostCanBeBooted() {
		// Arrange:
		try (final NisPeerNetworkHost host = createNetwork()) {
			// Act:
			host.boot(createLocalNode()).join();
		}
	}

	@Test
	public void defaultHostCanBeBootedAsync() {
		// Arrange:
		try (final NisPeerNetworkHost host = createNetwork()) {
			// Act:
			final CompletableFuture future = host.boot(createLocalNode());

			// Assert:
			Assert.assertThat(future.isDone(), IsEqual.equalTo(false));

			// Cleanup:
			future.join();
		}
	}

	@Test
	public void getNetworkThrowsIfNetworkIsNotBooted() {
		// Arrange:
		try (final NisPeerNetworkHost host = createNetwork()) {
			// Act:
			NisUtils.assertThrowsNisIllegalStateException(
					v -> host.getNetwork(),
					NisIllegalStateException.Reason.NIS_ILLEGAL_STATE_NOT_BOOTED);
		}
	}

	@Test
	public void getNetworkDoesNotThrowIfNetworkIsBooted() {
		// Arrange:
		try (final NisPeerNetworkHost host = createNetwork()) {
			// Act:
			host.boot(createLocalNode()).join();

			// Assert:
			Assert.assertThat(host.getNetwork(), IsNull.notNullValue());
		}
	}

	//region isNetworkBooted

	@Test
	public void isNetworkBootedReturnsFalseIfNetworkIsNotBooted() {
		// Arrange:
		final NisPeerNetworkHost host = createNetwork();

		// Assert:
		Assert.assertThat(host.isNetworkBooted(), IsEqual.equalTo(false));
	}

	@Test
	public void isNetworkBootedReturnsTrueIfNetworkIsBooted() {
		// Arrange:
		final NisPeerNetworkHost host = createNetwork();

		// Act:
		host.boot(createLocalNode()).join();

		// Assert:
		Assert.assertThat(host.isNetworkBooted(), IsEqual.equalTo(true));
	}

	//endregion

	//region isNetworkBooting

	@Test
	public void isNetworkBootingReturnsFalseIfNetworkIsNotBooted() {
		// Arrange:
		final NisPeerNetworkHost host = createNetwork();

		// Assert:
		Assert.assertThat(host.isNetworkBooting(), IsEqual.equalTo(false));
	}

	@Test
	public void isNetworkBootingReturnsTrueIfNetworkIsBooting() {
		// Arrange:
		final NisPeerNetworkHost host = createNetwork();

		// Act:
		final CompletableFuture<?> future = host.boot(createLocalNode());

		// Assert:
		Assert.assertThat(host.isNetworkBooting(), IsEqual.equalTo(true));

		// Cleanup:
		future.join();
	}

	@Test
	public void isNetworkBootingReturnsFalseIfNetworkIsBooted() {
		// Arrange:
		final NisPeerNetworkHost host = createNetwork();

		// Act:
		host.boot(createLocalNode()).join();

		// Assert:
		Assert.assertThat(host.isNetworkBooting(), IsEqual.equalTo(false));
	}

	//endregion

	@Test(expected = NisIllegalStateException.class)
	public void networkCannotBeBootedMoreThanOnce() {
		// Arrange:
		try (final NisPeerNetworkHost host = createNetwork()) {
			// Act:
			host.boot(createLocalNode());
			host.boot(createLocalNode());
		}
	}

	@Test
	public void getVisitorsReturnsEightTimerVisitors() {
		// Arrange:
		try (final NisPeerNetworkHost host = createNetwork()) {
			// Act:
			host.boot(createLocalNode()).join();
			final List<NemAsyncTimerVisitor> visitors = host.getVisitors();

			// Assert:
			Assert.assertThat(visitors.size(), IsEqual.equalTo(8));
		}
	}

	private static Node createLocalNode() {
		return new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("10.0.0.1"));
	}

	private static NisPeerNetworkHost createNetwork() {
		final ReadOnlyNisCache nisCache = Mockito.mock(ReadOnlyNisCache.class);
		Mockito.when(nisCache.getPoiFacade()).thenReturn(Mockito.mock(PoiFacade.class));

		final TimeProvider timeProvider = new SystemTimeProvider();
		final AuditCollection auditCollection = new AuditCollection(10, timeProvider);
		return new NisPeerNetworkHost(
				nisCache,
				Mockito.mock(CountingBlockSynchronizer.class),
				new PeerNetworkScheduler(timeProvider, Mockito.mock(HarvestingTask.class)),
				Mockito.mock(ChainServices.class),
				Mockito.mock(NodeCompatibilityChecker.class),
				new NisConfiguration(),
				new HttpConnectorPool(CommunicationMode.JSON, auditCollection),
				NisUtils.createTrustProvider(),
				auditCollection,
				auditCollection);
	}
}