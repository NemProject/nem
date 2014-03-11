package org.nem.peer;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.peer.test.*;

public class PeerNetworkHostTest {

    @Test
    public void defaultHostCanBeCreated() {
        // Arrange:
        final PeerNetworkHost host = PeerNetworkHost.getDefaultHost();

        // Assert:
        Assert.assertThat(host, IsNot.not(IsEqual.equalTo(null)));
    }

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

    private static class MockPeerNetwork extends PeerNetwork {

        int numRefreshCalls;

        public MockPeerNetwork() {
            super(ConfigFactory.createDefaultTestConfig(), new MockPeerConnector(), new MockNodeSchedulerFactory());
        }

        public int getNumRefreshCalls() { return this.numRefreshCalls; }

        @Override
        public void refresh() {
            ++numRefreshCalls;
            super.refresh();
        }
    }
}