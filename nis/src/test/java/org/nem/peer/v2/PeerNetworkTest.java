package org.nem.peer.v2;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.IsEquivalent;

import java.util.*;

public class PeerNetworkTest {

    //region constructor

    @Test
    public void ctorAddsAllWellKnownPeersAsInactive() {
        // Act:
        final PeerNetwork network = createTestNetwork();
        final NodeStatusDemux demux = network.getNodes();

        // Assert:
        assertStatusListNodes(demux, new String[] { }, new String[] { "10.0.0.1", "10.0.0.2", "10.0.0.3" });
    }

    @Test
    public void ctorDoesNotTriggerConnectorCalls() {
        // Act:
        final MockPeerConnector connector = new MockPeerConnector();
        createTestNetwork(connector);

        // Assert:
        Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(0));
        Assert.assertThat(connector.getNumGetKnownPeerCalls(), IsEqual.equalTo(0));
    }

    @Test
    public void ctorInitializesNodePlatformToUnknown() {
        // Act:
        final PeerNetwork network = createTestNetwork();
        final NodeStatusDemux demux = network.getNodes();

        // Assert:
        Assert.assertThat(demux.getInactiveNodes().size(), IsEqual.equalTo(3));
        for (final Node node : demux.getInactiveNodes())
            Assert.assertThat(node.getPlatform(), IsEqual.equalTo("Unknown"));
    }

    @Test
    public void ctorInitializesNodeApplicationToUnknown() {
        // Act:
        final PeerNetwork network = createTestNetwork();
        final NodeStatusDemux demux = network.getNodes();

        // Assert:
        Assert.assertThat(demux.getInactiveNodes().size(), IsEqual.equalTo(3));
        for (final Node node : demux.getInactiveNodes())
            Assert.assertThat(node.getPlatform(), IsEqual.equalTo("Unknown"));
    }

    //endregion

    //region getLocalNode

    @Test
    public void getLocalNodeReturnsConfigLocalNode() {
        // Act:
        final Config config = new Config(createTestJsonConfig());
        final PeerNetwork network = new PeerNetwork(config, new MockPeerConnector());

        // Assert:
        Assert.assertThat(network.getLocalNode(), IsEqual.equalTo(config.getLocalNode()));
    }

    //endregion

    //region refresh

    @Test
    public void refreshCallsGetInfoForEveryNode() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);

        // Act:
        network.refresh();

        // Assert:
        Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(3));
    }

    @Test
    public void refreshSuccessMovesNodesToActive() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetInfoError("10.0.0.2", MockPeerConnector.TriggerAction.NONE);

        // Act:
        network.refresh();
        final NodeStatusDemux demux = network.getNodes();

        // Assert:
        assertStatusListNodes(demux, new String[]{ "10.0.0.1", "10.0.0.3", "10.0.0.2" }, new String[]{ });
    }
   
    @Test
    public void refreshTransientFailureMovesNodesToInactive() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetInfoError("10.0.0.2", MockPeerConnector.TriggerAction.INACTIVE);

        // Act:
        network.refresh();
        final NodeStatusDemux demux = network.getNodes();

        // Assert:
        assertStatusListNodes(demux, new String[]{ "10.0.0.1", "10.0.0.3" }, new String[]{ "10.0.0.2" });
    }

    @Test
    public void refreshFatalFailureRemovesNodesFromBothLists() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetInfoError("10.0.0.2", MockPeerConnector.TriggerAction.FATAL);

        // Act:
        network.refresh();
        final NodeStatusDemux demux = network.getNodes();

        // Assert:
        assertStatusListNodes(demux, new String[]{ "10.0.0.1", "10.0.0.3" }, new String[]{ });
    }

    //endregion

    // TODO: refactor
    private static void assertStatusListNodes(
        final NodeStatusDemux demux,
        final String[] expectedActiveHosts,
        final String[] expectedInactiveHosts) {
        // Assert:
        Assert.assertThat(getHosts(demux.getActiveNodes()), IsEquivalent.equivalentTo(expectedActiveHosts));
        Assert.assertThat(getHosts(demux.getInactiveNodes()), IsEquivalent.equivalentTo(expectedInactiveHosts));
    }

    private static List<String> getHosts(final Collection<Node> nodes) {
        final List<String> platforms = new ArrayList<>();
        for (final Node node : nodes)
            platforms.add(node.getEndpoint().getBaseUrl().getHost());
        return platforms;
    }

    private static PeerNetwork createTestNetwork(final PeerConnector connector) {
        return new PeerNetwork(new Config(createTestJsonConfig()), connector);
    }

    private static PeerNetwork createTestNetwork() {
        return createTestNetwork(new MockPeerConnector());
    }

    //region mocks

    // TODO: refactor (copied from configtest.java)
    private static JSONObject createEndpointJsonObject(final String protocol, final String host, final int port) {
        JSONObject jsonEndpoint = new JSONObject();
        jsonEndpoint.put("protocol", protocol);
        jsonEndpoint.put("host", host);
        jsonEndpoint.put("port", port);
        return jsonEndpoint;
    }

    private static JSONObject createTestJsonConfig(final String[] hostNames) {
        JSONObject jsonConfig = new JSONObject();

        JSONObject jsonEndpoint = new JSONObject();
        jsonEndpoint.put("protocol", "http");
        jsonEndpoint.put("host", "10.0.0.8");
        jsonEndpoint.put("port", 7890);
        jsonConfig.put("endpoint", jsonEndpoint);

        jsonConfig.put("platform", "Mac");
        jsonConfig.put("application", "FooBar");

        JSONArray jsonWellKnownPeers = new JSONArray();
        for (final String hostName : hostNames)
            jsonWellKnownPeers.add(createEndpointJsonObject("ftp", hostName, 12));
        jsonConfig.put("knownPeers", jsonWellKnownPeers);
        return jsonConfig;
    }

    private static JSONObject createTestJsonConfig() {
        return createTestJsonConfig(new String[] { "10.0.0.1", "10.0.0.3", "10.0.0.2" });
    }

    private static class MockPeerConnector implements PeerConnector {

        private int numGetInfoCalls;
        private int numGetKnownPeerCalls;

        private String getErrorTrigger;
        public TriggerAction getErrorTriggerAction;

        public enum TriggerAction {
            NONE,
            INACTIVE,
            FATAL
        }

        public int getNumGetInfoCalls() { return this.numGetInfoCalls; }
        public int getNumGetKnownPeerCalls() { return this.numGetKnownPeerCalls; }

        public void setGetInfoError(final String trigger, final TriggerAction action) {
            this.getErrorTrigger = trigger;
            this.getErrorTriggerAction = action;
        }

        @Override
        public Node getInfo(final NodeEndpoint endpoint) {
            ++this.numGetInfoCalls;

            if (endpoint.getBaseUrl().getHost().equals(this.getErrorTrigger)) {
                switch (this.getErrorTriggerAction) {
                    case INACTIVE:
                        throw new InactivePeerException("inactive peer");

                    case FATAL:
                        throw new FatalPeerException("fatal peer");
                }
            }

            return new Node(endpoint, "P", "A");
        }

        @Override
        public NodeStatusDemux getKnownPeers(final NodeEndpoint endpoint) {
            ++numGetKnownPeerCalls;
            return null;
        }
    }

    //endregion
}
