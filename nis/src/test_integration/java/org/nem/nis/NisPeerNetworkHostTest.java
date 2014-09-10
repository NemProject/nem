package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.crypto.KeyPair;
import org.nem.core.node.*;
import org.nem.deploy.NisConfiguration;

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

	@Test(expected = IllegalStateException.class)
	public void getNetworkThrowsIfNetworkIsNotBooted() {
		// Arrange:
		try (final NisPeerNetworkHost host = createNetwork()) {
			// Act:
			host.getNetwork();
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

	@Test(expected = IllegalStateException.class)
	public void networkCannotBeBootedMoreThanOnce() {
		// Arrange:
		try (final NisPeerNetworkHost host = createNetwork()) {
			// Act:
			host.boot(createLocalNode());
			host.boot(createLocalNode());
		}
	}

	@Test
	public void getVisitorsReturnsSixTimerVisitors() {
		// Arrange:
		try (final NisPeerNetworkHost host = createNetwork()) {
			// Act:
			host.boot(createLocalNode()).join();
			final List<NemAsyncTimerVisitor> visitors = host.getVisitors();

			// Assert:
			Assert.assertThat(visitors.size(), IsEqual.equalTo(6));
		}
	}

	private static Node createLocalNode() {
		return new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("10.0.0.1"));
	}

	private static NisPeerNetworkHost createNetwork() {
		return new NisPeerNetworkHost(null, null, new NisConfiguration());
	}
}