package org.nem.peer;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.connect.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.utils.ExceptionUtils;
import org.nem.peer.connect.*;
import org.nem.peer.node.*;
import org.nem.peer.test.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class NodeRefresherTest {
	//region getInfo

	//region call counts

	@Test
	public void refreshCallsGetInfoForEveryInactiveNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		updateAllNodes(network, NodeStatus.INACTIVE);
		network.getNodes().update(PeerUtils.createNodeWithHost("10.0.0.25"), NodeStatus.INACTIVE);

		// Act:
		network.refresh().join();

		// Assert:
		Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(4));
	}

	@Test
	public void refreshCallsGetInfoForEveryActiveNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		updateAllNodes(network, NodeStatus.ACTIVE);
		network.getNodes().update(PeerUtils.createNodeWithHost("10.0.0.25"), NodeStatus.ACTIVE);

		// Act:
		network.refresh().join();

		// Assert:
		Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(4));
	}

	@Test
	public void refreshDoesNotCallGetInfoForNonPreTrustedFailedNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		network.getNodes().update(PeerUtils.createNodeWithHost("10.0.0.25"), NodeStatus.FAILURE);

		// Act:
		network.refresh().join();

		// Assert:
		Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(3));
	}

	@Test
	public void refreshCallsGetInfoForPreTrustedFailedNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		updateAllNodes(network, NodeStatus.FAILURE);

		// Act:
		network.refresh().join();

		// Assert:
		Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(3));
	}

	private static void updateAllNodes(final PeerNetwork network, final NodeStatus status) {
		// Arrange:
		final NodeCollection nodes = network.getNodes();
		nodes.getAllNodes().stream().forEach(node -> nodes.update(node, status));
	}

	//endregion

	//region transitions

	@Test
	public void refreshSuccessMovesNodesToActive() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.NONE);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(nodes, new String[] { "10.0.0.1", "10.0.0.3", "10.0.0.2" }, new String[] { });
	}

	@Test
	public void refreshGetInfoTransientFailureMovesNodesToInactive() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.INACTIVE);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(nodes, new String[] { "10.0.0.1", "10.0.0.3" }, new String[] { "10.0.0.2" });
	}

	@Test
	public void refreshGetInfoFatalFailureRemovesNodesFromBothLists() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.FATAL);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(nodes, new String[] { "10.0.0.1", "10.0.0.3" }, new String[] { });
	}

	@Test
	public void refreshGetInfoChangeIdentityRemovesNodesFromBothLists() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		updateAllNodes(network, NodeStatus.ACTIVE);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.CHANGE_IDENTITY);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(nodes, new String[] { "10.0.0.1", "10.0.0.3" }, new String[] { });
	}

	@Test
	public void refreshGetInfoChangeAddressUpdatesNodeEndpoint() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		updateAllNodes(network, NodeStatus.ACTIVE);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.CHANGE_ADDRESS);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(
				nodes,
				new String[] { "10.0.0.1", "10.0.0.20", "10.0.0.3" },
				new String[] { });
	}

	@Test
	public void refreshGetInfoChangeMetaDataUpdatesNodeMetaData() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		updateAllNodes(network, NodeStatus.ACTIVE);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.CHANGE_METADATA);

		// Act:
		network.refresh().join();
		final Node node = network.getNodes().findNodeByIdentity(new WeakNodeIdentity("10.0.0.2"));
		final NodeMetaData metaData = node.getMetaData();

		// Assert:
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("c-app"));
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("c-plat"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo("c-ver"));
	}

	//endregion

	//endregion

	//region getKnownPeers

	@Test
	public void refreshCallsGetKnownPeersForActiveNodes() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);

		// Act:
		network.refresh().join();

		// Assert:
		Assert.assertThat(connector.getNumGetKnownPeerCalls(), IsEqual.equalTo(3));
	}

	@Test
	public void refreshCallsGetKnownPeersForChangeAddressNodes() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.CHANGE_ADDRESS);

		// Act:
		network.refresh().join();

		// Assert:
		Assert.assertThat(connector.getNumGetKnownPeerCalls(), IsEqual.equalTo(3));
	}

	@Test
	public void refreshDoesNotCallGetKnownPeersForInactiveNodes() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.INACTIVE);

		// Act:
		network.refresh().join();

		// Assert:
		Assert.assertThat(connector.getNumGetKnownPeerCalls(), IsEqual.equalTo(2));
	}

	@Test
	public void refreshDoesNotCallGetKnownPeersForFatalFailureNodes() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.FATAL);

		// Act:
		network.refresh().join();

		// Assert:
		Assert.assertThat(connector.getNumGetKnownPeerCalls(), IsEqual.equalTo(2));
	}

	@Test
	public void refreshDoesNotCallGetKnownPeersForChangeIdentityNodes() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.CHANGE_IDENTITY);

		// Act:
		network.refresh().join();

		// Assert:
		Assert.assertThat(connector.getNumGetKnownPeerCalls(), IsEqual.equalTo(2));
	}

	@Test
	public void refreshGetKnownPeersTransientFailureMovesNodesToInactive() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetKnownPeersError("10.0.0.2", MockConnector.TriggerAction.INACTIVE);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(nodes, new String[] { "10.0.0.1", "10.0.0.3" }, new String[] { "10.0.0.2" });
	}

	@Test
	public void refreshGetKnownPeersFatalFailureRemovesNodesFromBothLists() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetKnownPeersError("10.0.0.2", MockConnector.TriggerAction.FATAL);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(nodes, new String[] { "10.0.0.1", "10.0.0.3" }, new String[] { });
	}

	@Test
	public void refreshGivesPrecedenceToFirstHandExperience() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.SLEEP_INACTIVE);
		connector.setGetInfoError("10.0.0.4", MockConnector.TriggerAction.FATAL);
		connector.setGetInfoError("10.0.0.6", MockConnector.TriggerAction.INACTIVE);
		connector.setGetInfoError("10.0.0.7", MockConnector.TriggerAction.CHANGE_IDENTITY);

		// Arrange: set up a node peers list that indicates peer 10.0.0.2, 10.0.0.4-7 are active
		// but the local node can only communicate with 10.0.0.5
		final List<Node> knownPeers = Arrays.asList(
				PeerUtils.createNodeWithHost("10.0.0.2"),
				PeerUtils.createNodeWithHost("10.0.0.4"),
				PeerUtils.createNodeWithHost("10.0.0.5"),
				PeerUtils.createNodeWithHost("10.0.0.6"),
				PeerUtils.createNodeWithHost("10.0.0.7"));
		connector.setKnownPeers(knownPeers);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(
				nodes,
				new String[] { "10.0.0.1", "10.0.0.3", "10.0.0.5" },
				new String[] { "10.0.0.2", "10.0.0.6" });
	}

	@Test
	public void refreshPreventsEvilNodeFromGettingGoodNodesDropped() {
		// this is similar to refreshGivesPrecedenceToFirstHandExperience
		// but is important to show the following attack is prevented:
		// evil node propagating mismatched identities for good nodes does not remove the good nodes

		// Arrange: set up 4 active nodes (3 pre-trusted)
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		updateAllNodes(network, NodeStatus.ACTIVE);
		network.getNodes().update(PeerUtils.createNodeWithHost("10.0.0.25"), NodeStatus.ACTIVE);
		connector.setGetInfoError("10.0.0.3", MockConnector.TriggerAction.SLEEP_INACTIVE);

		// when the mock connector sees hosts 100-3, it will trigger an identity change
		connector.setGetInfoError("10.0.0.100", MockConnector.TriggerAction.CHANGE_IDENTITY);
		connector.setGetInfoError("10.0.0.101", MockConnector.TriggerAction.CHANGE_IDENTITY);
		connector.setGetInfoError("10.0.0.102", MockConnector.TriggerAction.CHANGE_IDENTITY);
		connector.setGetInfoError("10.0.0.103", MockConnector.TriggerAction.CHANGE_IDENTITY);

		// Arrange: set up a node peers list that indicates good peers (1, 3, 25, 26) are untrustworthy
		// (2 is the evil node in this scenario)
		final List<Node> knownPeers = Arrays.asList(
				PeerUtils.createNodeWithHost("10.0.0.100", "10.0.0.1"),
				PeerUtils.createNodeWithHost("10.0.0.101", "10.0.0.3"),
				PeerUtils.createNodeWithHost("10.0.0.102", "10.0.0.25"),
				PeerUtils.createNodeWithHost("10.0.0.103", "10.0.0.26"));
		connector.setKnownPeers(knownPeers);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		// - all good peers (1, 3, 25) that were directly communicated with are active
		// - the evil node (2) is active because the reverse is possible (i.e. 2 is the only good node)
		// - the good node that hasn't been communicated with (26) has been dropped
		NodeCollectionAssert.areHostsEquivalent(
				nodes,
				new String[] { "10.0.0.1", "10.0.0.25", "10.0.0.2" },
				new String[] { "10.0.0.3" });
	}

	@Test
	public void refreshResultForDirectNodeIgnoresChildNodeGetInfoResults() {
		// Arrange: set up 4 active nodes (3 pre-trusted)
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		updateAllNodes(network, NodeStatus.ACTIVE);
		network.getNodes().update(PeerUtils.createNodeWithHost("10.0.0.25"), NodeStatus.ACTIVE);

		connector.setGetInfoError("10.0.0.3", MockConnector.TriggerAction.SLEEP_INACTIVE);
		connector.setGetInfoError("10.0.0.100", MockConnector.TriggerAction.INACTIVE);

		// Arrange: set up a node peers list that contains an unseen inactive node
		final List<Node> knownPeers = Arrays.asList(PeerUtils.createNodeWithHost("10.0.0.100"));
		connector.setKnownPeers(knownPeers);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		// - all peers (1, 2, 25) that were directly communicated (successfully) are active
		// - the inactive peer (3) is inactive
		// - the unseen inactive peer (100) is inactive
		NodeCollectionAssert.areHostsEquivalent(
				nodes,
				new String[] { "10.0.0.1", "10.0.0.2", "10.0.0.25" },
				new String[] { "10.0.0.3", "10.0.0.100" });
	}

	@Test
	public void refreshCallsGetInfoOnceForEachUniqueEndpoint() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.SLEEP_INACTIVE);
		connector.setGetInfoError("10.0.0.4", MockConnector.TriggerAction.FATAL);
		connector.setGetInfoError("10.0.0.6", MockConnector.TriggerAction.INACTIVE);

		// Arrange: set up a node peers list that indicates peer 10.0.0.2, 10.0.0.4-6 are active
		// but the local node can only communicate with 10.0.0.5
		final List<Node> knownPeers = Arrays.asList(
				PeerUtils.createNodeWithHost("10.0.0.2"),
				PeerUtils.createNodeWithHost("10.0.0.4"),
				PeerUtils.createNodeWithHost("10.0.0.5"),
				PeerUtils.createNodeWithHost("10.0.0.6"));
		connector.setKnownPeers(knownPeers);

		// Act:
		network.refresh().join();
		network.getNodes();

		// Assert:
		Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(6));
	}

	@Test
	public void refreshOnlyMergesInRelayedActivePeers() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);

		final List<Node> knownPeers = Arrays.asList(
				PeerUtils.createNodeWithHost("10.0.0.15"),
				PeerUtils.createNodeWithHost("10.0.0.6"));
		connector.setKnownPeers(knownPeers);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(
				nodes,
				new String[] { "10.0.0.1", "10.0.0.2", "10.0.0.3", "10.0.0.6", "10.0.0.15" },
				new String[] { });
	}

	@Test
	public void refreshDoesNotMergeInLocalNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);

		final List<Node> knownPeers = Arrays.asList(network.getLocalNode());
		connector.setKnownPeers(knownPeers);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(
				nodes,
				new String[] { "10.0.0.1", "10.0.0.2", "10.0.0.3" },
				new String[] { });
	}

	//endregion
}