package org.nem.peer;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.test.*;
import org.nem.peer.trust.NodeExperiencesPair;

public class PeerNetworkHostTest {

	@Test
	public void hostCanBeCreatedAroundCustomNetwork() throws Exception {
		// Arrange:
		final PeerNetwork network = new MockPeerNetwork();
		try (final PeerNetworkHost host = new PeerNetworkHost(network, 100, 100)) {
			// Assert:
			Assert.assertThat(host, IsNot.not(IsEqual.equalTo(null)));
			Assert.assertThat(host.getNetwork(), IsEqual.equalTo(network));
		}
	}

	@Test
	public void initialRefreshRateIsRespected() throws Exception {
		// Arrange:
		final MockPeerNetwork network = new MockPeerNetwork();
		try (final PeerNetworkHost ignored = new PeerNetworkHost(network, 50, 100)) {
			// Arrange:
			Thread.sleep(25);

			// Assert:
			Assert.assertThat(network.getNumRefreshCalls(), IsEqual.equalTo(0));

			// Arrange:
			Thread.sleep(75);

			// Assert:
			Assert.assertThat(network.getNumRefreshCalls(), IsEqual.equalTo(1));
		}
	}

	@Test
	public void refreshIntervalIsRespected() throws Exception {
		// Arrange:
		final MockPeerNetwork network = new MockPeerNetwork();
		try (final PeerNetworkHost ignored = new PeerNetworkHost(network, 10, 20)) {
			// Arrange:
			Thread.sleep(60);

			// Assert:
			Assert.assertThat(network.getNumRefreshCalls(), IsEqual.equalTo(3));
		}
	}

	@Test
	public void shutdownStopsRefreshing() throws Exception {
		// Arrange:
		final MockPeerNetwork network = new MockPeerNetwork();
		try (final PeerNetworkHost ignored = new PeerNetworkHost(network, 10, 20)) {
			// Arrange:
			Thread.sleep(15);

			// Assert:
			Assert.assertThat(network.getNumRefreshCalls(), IsEqual.equalTo(1));
		}

		// Arrange:
		Thread.sleep(45);

		// Assert:
		Assert.assertThat(network.getNumRefreshCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void hostThrottlesRefresh() throws Exception {
		// Arrange:
		final Object refreshMonitor = new Object();
		final MockPeerNetwork network = new MockPeerNetwork(refreshMonitor);
		try (final PeerNetworkHost ignored = new PeerNetworkHost(network, 10, 20)) {
			// Arrange: (expect calls at 10, 30, 50)
			Thread.sleep(60);

			// Act: signal the monitor (one thread should be unblocked)
			Utils.monitorSignal(refreshMonitor);
			Thread.sleep(5);

			// Assert:
			Assert.assertThat(network.getNumRefreshCalls(), IsEqual.equalTo(1));
		}
	}

	@Test
	public void refreshCallsBroadcastWithLocalNode() throws Exception {
		// Arrange:
		final MockPeerNetwork network = new MockPeerNetwork();
		try (final PeerNetworkHost ignored = new PeerNetworkHost(network, 10, 100)) {
			// Arrange:
			Thread.sleep(25);
			final NodeExperiencesPair broadcastEntity = (NodeExperiencesPair)network.getLastBroadcastEntity();

			// Assert:
			Assert.assertThat(network.getNumBroadcastCalls(), IsEqual.equalTo(1));
			Assert.assertThat(network.getLastBroadcastId(), IsEqual.equalTo(NodeApiId.REST_NODE_PING));
			Assert.assertThat(broadcastEntity.getNode(), IsSame.sameInstance(network.getLocalNode()));
		}
	}

	@Test
	public void refreshCallsSynchronize() throws Exception {
		// Arrange:
		final MockPeerNetwork network = new MockPeerNetwork();
		try (final PeerNetworkHost ignored = new PeerNetworkHost(network, 10, 100)) {
			// Arrange:
			Thread.sleep(25);

			// Assert:
			Assert.assertThat(network.getNumSynchronizeCalls(), IsEqual.equalTo(1));
		}
	}
}