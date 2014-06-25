package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.test.ExceptionAssert;
import org.nem.peer.Config;
import org.nem.peer.node.*;
import org.nem.peer.trust.PreTrustedNodes;

import java.util.*;
import java.util.concurrent.*;

public class NisPeerNetworkHostTest {

	@Test
	public void defaultHostCanBeBooted() {
		// Arrange:
		try (final NisPeerNetworkHost host = new NisPeerNetworkHost(null, null)) {
			// Act:
			host.boot(createLocalNode()).join();
		}
	}

	@Test
	public void defaultHostCanBeBootedAsync() {
		// Arrange:
		try (final NisPeerNetworkHost host = new NisPeerNetworkHost(null, null)) {
			// Act:
			final CompletableFuture future = host.boot(createLocalNode());

			// Assert:
			Assert.assertThat(future.isDone(), IsEqual.equalTo(false));

			// Cleanup:
			future.join();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void getNetworkThrowsIfNetworkIsNotBooted() {
		// Arrange:
		try (final NisPeerNetworkHost host = new NisPeerNetworkHost(null, null)) {
			// Act:
			host.getNetwork();
		}
	}

	@Test
	public void getNetworkDoesNotThrowIfNetworkIsBooted() {
		// Arrange:
		try (final NisPeerNetworkHost host = new NisPeerNetworkHost(null, null)) {
			// Act:
			host.boot(createLocalNode()).join();

			// Assert:
			Assert.assertThat(host.getNetwork(), IsNull.notNullValue());
		}
	}

	@Test(expected = IllegalStateException.class)
	public void networkCannotBeBootedMoreThanOnce() {
		// Arrange:
		try (final NisPeerNetworkHost host = new NisPeerNetworkHost(null, null)) {
			// Act:
			host.boot(createLocalNode());
			host.boot(createLocalNode());
		}
	}

	@Test
	public void bootCanBeRetriedIfInitialBootFails() {
		// Arrange:
		final Config config = Mockito.mock(Config.class);
		Mockito.when(config.getPreTrustedNodes()).thenReturn(new PreTrustedNodes(new HashSet<>()));

		try (final NisPeerNetworkHost host = new NisPeerNetworkHost(null, null)) {
			// Arrange: trigger a boot failure
			ExceptionAssert.assertThrowsCompletionException(v -> host.boot(config).join(), IllegalStateException.class);

			// Act:
			host.boot(createLocalNode()).join();

			// Assert:
			Assert.assertThat(host.getNetwork(), IsNull.notNullValue());
		}
	}

	@Test
	public void getVisitorsReturnsSixTimerVisitors() {
		// Arrange:
		try (final NisPeerNetworkHost host = new NisPeerNetworkHost(null, null)) {
			// Act:
			host.boot(createLocalNode()).join();
			final List<NisAsyncTimerVisitor> visitors = host.getVisitors();

			// Assert:
			Assert.assertThat(visitors.size(), IsEqual.equalTo(6));
		}
	}

	private static Node createLocalNode() {
		return new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("10.0.0.1"));
	}
}
