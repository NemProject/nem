package org.nem.peer;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.test.MockSerializableEntity;
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

    //region getInfo

    @Test
    public void refreshCallsGetInfoForEveryInactiveNode() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);

        // Act:
        network.refresh();

        // Assert:
        Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(3));
    }

    @Test
    public void refreshCallsGetInfoForEveryActiveNode() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);

        // Act:
        network.refresh(); // transition all nodes to active
        network.refresh();

        // Assert:
        Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(6));
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
    public void refreshGetInfoTransientFailureMovesNodesToInactive() {
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
    public void refreshGetInfoFatalFailureRemovesNodesFromBothLists() {
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

    @Test
    public void refreshNodeChangeAddressRemovesNodesFromBothLists() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetInfoError("10.0.0.2", MockPeerConnector.TriggerAction.CHANGE_ADDRESS);

        // Act:
        network.refresh();
        final NodeCollection nodes = network.getNodes();

        // Assert:
        NodeCollectionAssert.areHostsEquivalent(nodes, new String[]{ "10.0.0.1", "10.0.0.3" }, new String[]{ });
    }

    //endregion

    //region getKnownPeers

    @Test
    public void refreshCallsGetKnownPeersForActiveNodes() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);

        // Act:
        network.refresh();

        // Assert:
        Assert.assertThat(connector.getNumGetKnownPeerCalls(), IsEqual.equalTo(3));
    }

    @Test
    public void refreshDoesNotCallGetKnownPeersForInactiveNodes() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetInfoError("10.0.0.2", MockPeerConnector.TriggerAction.INACTIVE);

        // Act:
        network.refresh();

        // Assert:
        Assert.assertThat(connector.getNumGetKnownPeerCalls(), IsEqual.equalTo(2));
    }

    @Test
    public void refreshDoesNotCallGetKnownPeersForFatalFailureNodes() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetInfoError("10.0.0.2", MockPeerConnector.TriggerAction.FATAL);

        // Act:
        network.refresh();

        // Assert:
        Assert.assertThat(connector.getNumGetKnownPeerCalls(), IsEqual.equalTo(2));
    }

    @Test
    public void refreshDoesNotCallGetKnownPeersForChangeAddressNodes() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetInfoError("10.0.0.2", MockPeerConnector.TriggerAction.CHANGE_ADDRESS);

        // Act:
        network.refresh();

        // Assert:
        Assert.assertThat(connector.getNumGetKnownPeerCalls(), IsEqual.equalTo(2));
    }

    @Test
    public void refreshGetKnownPeersTransientFailureMovesNodesToInactive() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetKnownPeersError("10.0.0.2", MockPeerConnector.TriggerAction.INACTIVE);

        // Act:
        network.refresh();
        final NodeCollection nodes = network.getNodes();

        // Assert:
        NodeCollectionAssert.areHostsEquivalent(nodes, new String[]{ "10.0.0.1", "10.0.0.3" }, new String[]{ "10.0.0.2" });
    }

    @Test
    public void refreshGetKnownPeersFatalFailureRemovesNodesFromBothLists() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetKnownPeersError("10.0.0.2", MockPeerConnector.TriggerAction.FATAL);

        // Act:
        network.refresh();
        final NodeCollection nodes = network.getNodes();

        // Assert:
        NodeCollectionAssert.areHostsEquivalent(nodes, new String[]{ "10.0.0.1", "10.0.0.3" }, new String[]{ });
    }

    @Test
    public void refreshMergesInKnownPeers() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        connector.setGetInfoError("10.0.0.2", MockPeerConnector.TriggerAction.INACTIVE);

        // Arrange: set up a node peers list that indicates the reverse of direct communication
        // (i.e. 10.0.0.2 is active and all other nodes are inactive)
        NodeCollection knownPeers = new NodeCollection();
        knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.1", 12), "p", "a"), NodeStatus.INACTIVE);
        knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.2", 12), "p", "a"), NodeStatus.ACTIVE);
        knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.3", 12), "p", "a"), NodeStatus.INACTIVE);
        connector.setKnownPeers(knownPeers);

        // Act:
        network.refresh();
        final NodeCollection nodes = network.getNodes();

        // Assert:
        NodeCollectionAssert.areHostsEquivalent(nodes, new String[]{ "10.0.0.1", "10.0.0.3" }, new String[]{ "10.0.0.2" });
    }

    @Test
    public void refreshGivesPrecedenceToFirstHandExperience() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);

        NodeCollection knownPeers = new NodeCollection();
        knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.15", 12), "p", "a"), NodeStatus.ACTIVE);
        knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.7", 12), "p", "a"), NodeStatus.INACTIVE);
        knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.11", 12), "p", "a"), NodeStatus.INACTIVE);
        knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.8", 12), "p", "a"), NodeStatus.ACTIVE);
        connector.setKnownPeers(knownPeers);

        // Act:
        network.refresh();
        final NodeCollection nodes = network.getNodes();

        // Assert:
        NodeCollectionAssert.areHostsEquivalent(
            nodes,
            new String[]{ "10.0.0.1", "10.0.0.2", "10.0.0.3", "10.0.0.8", "10.0.0.15" },
            new String[]{ "10.0.0.7", "10.0.0.11" });
    }

    //endregion

    //region broadcast

    @Test
    public void broadcastDoesNotCallAnnounceForAnyInactiveNode() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);

        // Act:
        network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, new MockSerializableEntity());

        // Assert:
        Assert.assertThat(connector.getNumAnnounceCalls(), IsEqual.equalTo(0));
    }

    @Test
    public void broadcastCallsAnnounceForAllActiveNodes() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);

        // Act:
        network.refresh(); // transition all nodes to active
        network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, new MockSerializableEntity());

        // Assert:
        Assert.assertThat(connector.getNumAnnounceCalls(), IsEqual.equalTo(3));
    }

    @Test
    public void broadcastForwardsParametersToAnnounce() {
        // Arrange:
        final MockPeerConnector connector = new MockPeerConnector();
        final PeerNetwork network = createTestNetwork(connector);
        final SerializableEntity entity = new MockSerializableEntity();

        // Act:
        network.refresh(); // transition all nodes to active
        network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, entity);

        // Assert:
        Assert.assertThat(connector.getLastAnnounceId(), IsEqual.equalTo(NodeApiId.REST_PUSH_TRANSACTION));
        Assert.assertThat(connector.getLastAnnounceEntity(), IsEqual.equalTo(entity));
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
