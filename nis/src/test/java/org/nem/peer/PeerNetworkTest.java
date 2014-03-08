package org.nem.peer;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.peer.test.*;

public class PeerNetworkTest {

    //region constructor

    @Test
    public void ctorAddsAllWellKnownPeersAsInactive() {
        // Act:
        final PeerNetwork network = createTestNetwork();
        final NodeCollection nodes = network.getNodes();

        // Assert:
        NodeCollectionAssert.areHostsEquivalent(nodes, new String[] { }, new String[] { "10.0.0.1", "10.0.0.2", "10.0.0.3" });
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
        final NodeCollection nodes = network.getNodes();

        // Assert:
        Assert.assertThat(nodes.getInactiveNodes().size(), IsEqual.equalTo(3));
        for (final Node node : nodes.getInactiveNodes())
            Assert.assertThat(node.getPlatform(), IsEqual.equalTo("Unknown"));
    }

    @Test
    public void ctorInitializesNodeApplicationToUnknown() {
        // Act:
        final PeerNetwork network = createTestNetwork();
        final NodeCollection nodes = network.getNodes();

        // Assert:
        Assert.assertThat(nodes.getInactiveNodes().size(), IsEqual.equalTo(3));
        for (final Node node : nodes.getInactiveNodes())
            Assert.assertThat(node.getPlatform(), IsEqual.equalTo("Unknown"));
    }

    //endregion

    //region getLocalNode

    @Test
    public void getLocalNodeReturnsConfigLocalNode() {
        // Act:
        final Config config = createTestConfig();
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
        final NodeCollection nodes = network.getNodes();

        // Assert:
        NodeCollectionAssert.areHostsEquivalent(nodes, new String[]{ "10.0.0.1", "10.0.0.3", "10.0.0.2" }, new String[]{ });
    }
   
    @Test
    public void refreshTransientFailureMovesNodesToInactive() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetInfoError("10.0.0.2", MockPeerConnector.TriggerAction.INACTIVE);

        // Act:
        network.refresh();
        final NodeCollection nodes = network.getNodes();

        // Assert:
        NodeCollectionAssert.areHostsEquivalent(nodes, new String[]{ "10.0.0.1", "10.0.0.3" }, new String[]{ "10.0.0.2" });
    }

    @Test
    public void refreshFatalFailureRemovesNodesFromBothLists() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetInfoError("10.0.0.2", MockPeerConnector.TriggerAction.FATAL);

        // Act:
        network.refresh();
        final NodeCollection nodes = network.getNodes();

        // Assert:
        NodeCollectionAssert.areHostsEquivalent(nodes, new String[]{ "10.0.0.1", "10.0.0.3" }, new String[]{ });
    }

    //endregion

    //region factories

    private static PeerNetwork createTestNetwork(final PeerConnector connector) {
        return new PeerNetwork(createTestConfig(), connector);
    }

    private static PeerNetwork createTestNetwork() {
        return createTestNetwork(new MockPeerConnector());
    }

    private static Config createTestConfig() {
        return ConfigFactory.createDefaultTestConfig();
    }

    //endregion
}
