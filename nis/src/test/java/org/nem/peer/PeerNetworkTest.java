package org.nem.peer;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.ColumnVector;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.test.*;
import org.nem.peer.connect.PeerConnector;
import org.nem.peer.connect.SyncConnectorPool;
import org.nem.peer.node.*;
import org.nem.peer.test.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PeerNetworkTest {

	private static final String DEFAULT_LOCAL_NODE_HOST = "10.0.0.8";

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
		final MockConnector connector = new MockConnector();
		createTestNetwork(connector);

		// Assert:
		Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(0));
		Assert.assertThat(connector.getNumGetKnownPeerCalls(), IsEqual.equalTo(0));
	}

	@Test
	public void ctorInitializesNodeMetaDataToDefaultValues() {
		// Act:
		final PeerNetwork network = createTestNetwork();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		Assert.assertThat(nodes.getInactiveNodes().size(), IsEqual.equalTo(3));
		for (final Node node : nodes.getInactiveNodes()) {
			final NodeMetaData metaData = node.getMetaData();
			Assert.assertThat(metaData.getPlatform(), IsNull.nullValue());
			Assert.assertThat(metaData.getApplication(), IsNull.nullValue());
			Assert.assertThat(metaData.getApplication(), IsNull.nullValue());
		}
	}

	//endregion

	//region refresh

	//region getLocalNode

	@Test
	public void getLocalNodeReturnsConfigLocalNode() {
		// Act:
		final Config config = createTestConfig();
		final PeerNetwork network = new PeerNetwork(config, createMockPeerNetworkServices());

		// Assert:
		Assert.assertThat(network.getLocalNode(), IsEqual.equalTo(config.getLocalNode()));
	}

	//endregion

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

	@Test
	public void refreshUpdatesTrustValues() {
		// Arrange:
		final TrustProvider provider = Mockito.mock(TrustProvider.class);
		final PeerNetwork network = createTestNetwork(provider);

		final ColumnVector trustVector = new ColumnVector(network.getNodes().getAllNodes().size() + 1);
		trustVector.setAll(1);
		Mockito.when(provider.computeTrust(Mockito.any())).thenReturn(trustVector);

		// Act:
		network.refresh().join();

		// Assert:
		Mockito.verify(provider, Mockito.times(1)).computeTrust(Mockito.any());
	}

	//endregion

	//region broadcast

	@Test
	public void broadcastDoesNotCallAnnounceForAnyInactiveNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);

		// Act:
		network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, new MockSerializableEntity()).join();

		// Assert:
		Assert.assertThat(connector.getNumAnnounceCalls(), IsEqual.equalTo(0));
	}

	@Test
	public void broadcastCallsAnnounceForAllActiveNodes() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		network.refresh().join(); // transition all nodes to active

		// Act:
		network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, new MockSerializableEntity()).join();

		// Assert:
		Assert.assertThat(connector.getNumAnnounceCalls(), IsEqual.equalTo(3));
	}

	@Test
	public void broadcastForwardsParametersToAnnounce() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		final SerializableEntity entity = new MockSerializableEntity();
		network.refresh().join(); // transition all nodes to active

		// Act:
		network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, entity).join();

		// Assert:
		Assert.assertThat(connector.getLastAnnounceId(), IsEqual.equalTo(NodeApiId.REST_PUSH_TRANSACTION));
		Assert.assertThat(connector.getLastAnnounceEntity(), IsEqual.equalTo(entity));
	}

	//endregion

	//region synchronize

	@Test
	public void synchronizeDoesNotCallSynchronizeNodeForAnyInactiveNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final MockBlockSynchronizer synchronizer = new MockBlockSynchronizer();
		final PeerNetwork network = createTestNetwork(connector, synchronizer);
		for (final String host : new String[] { "10.0.0.1", "10.0.0.2", "10.0.0.3" })
			connector.setGetInfoError(host, MockConnector.TriggerAction.INACTIVE);
		network.refresh().join();

		// Act:
		network.synchronize();

		// Assert:
		Assert.assertThat(synchronizer.getNumSynchronizeNodeCalls(), IsEqual.equalTo(0));
	}

	@Test
	public void synchronizeCallsSynchronizeNodeForSingleCommunicationPartner() {
		// Arrange:
		final MockBlockSynchronizer synchronizer = new MockBlockSynchronizer();
		final PeerNetwork network = createTestNetwork(synchronizer);
		network.refresh().join(); // transition all nodes to active

		// Act:
		network.synchronize();

		// Assert:
		Assert.assertThat(synchronizer.getNumSynchronizeNodeCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void synchronizeForwardsValidParametersToSynchronizeNode() {
		// Arrange:
		final MockBlockSynchronizer synchronizer = new MockBlockSynchronizer();
		final PeerNetwork network = createTestNetwork(synchronizer);
		network.refresh().join(); // transition all nodes to active

		// Act:
		network.synchronize();

		// Assert:
		Assert.assertThat(synchronizer.getLastConnectorPool(), IsNull.notNullValue());
	}

	@Test
	public void synchronizeSyncsWithActiveNode() {
		// Act:
		final SynchronizeResult result = synchronizeActiveNode(NodeInteractionResult.SUCCESS);

		// Assert:
		Assert.assertThat(
				result.node.getEndpoint().getBaseUrl().getHost(),
				IsEqual.equalTo("10.0.0.2"));
	}

	@Test
	public void synchronizeUpdatesPartnerExperienceOnSuccess() {
		// Act:
		final SynchronizeResult result = synchronizeActiveNode(NodeInteractionResult.SUCCESS);

		// Assert:
		Assert.assertThat(result.experience.successfulCalls().get(), IsEqual.equalTo(1L));
		Assert.assertThat(result.experience.failedCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(result.experience.totalCalls(), IsEqual.equalTo(1L));
	}

	@Test
	public void synchronizeUpdatesPartnerExperienceOnFailure() {
		// Act:
		final SynchronizeResult result = synchronizeActiveNode(NodeInteractionResult.FAILURE);

		// Assert:
		Assert.assertThat(result.experience.successfulCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(result.experience.failedCalls().get(), IsEqual.equalTo(1L));
		Assert.assertThat(result.experience.totalCalls(), IsEqual.equalTo(1L));
	}

	@Test
	public void synchronizeDoesNotUpdatePartnerExperienceOnNeutral() {
		// Act:
		final SynchronizeResult result = synchronizeActiveNode(NodeInteractionResult.NEUTRAL);

		// Assert:
		Assert.assertThat(result.experience.successfulCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(result.experience.failedCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(result.experience.totalCalls(), IsEqual.equalTo(0L));
	}

	private static class SynchronizeResult {
		private Node node;
		private NodeExperience experience;
	}

	private static SynchronizeResult synchronizeActiveNode(final NodeInteractionResult synchronizeNodeResult) {
		// Arrange:
		final NodeExperiences nodeExperiences = new NodeExperiences();

		final MockBlockSynchronizer synchronizer = new MockBlockSynchronizer();
		synchronizer.setSynchronizeNodeResult(synchronizeNodeResult);

		final MockConnector connector = new MockConnector();
		connector.setGetInfoError("10.0.0.1", MockConnector.TriggerAction.INACTIVE);
		connector.setGetInfoError("10.0.0.3", MockConnector.TriggerAction.FATAL);

		final PeerNetwork network =  new PeerNetwork(
				createTestConfig(),
				new PeerNetworkServices(
						connector,
						Mockito.mock(SyncConnectorPool.class),
						synchronizer),
				nodeExperiences,
				new NodeCollection());

		network.refresh().join();

		// Act:
		network.synchronize();
		final SynchronizeResult result = new SynchronizeResult();
		result.node = synchronizer.getLastNode();
		result.experience = nodeExperiences.getNodeExperience(network.getLocalNode(), result.node);
		return result;
	}

	//endregion

	//region updateLocalNodeEndpoint

	@Test
	public void updateLocalNodeEndpointDoesNotCallGetLocalNodeInfoForAnyInactiveNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		for (final String host : new String[] { "10.0.0.1", "10.0.0.2", "10.0.0.3" })
			connector.setGetInfoError(host, MockConnector.TriggerAction.INACTIVE);
		network.refresh().join();

		// Act:
		network.updateLocalNodeEndpoint().join();

		// Assert:
		Assert.assertThat(connector.getNumGetLocalNodeInfoCalls(), IsEqual.equalTo(0));
	}

	@Test
	public void updateLocalNodeEndpointCallsGetLocalNodeInfoForSingleCommunicationPartner() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		network.refresh().join();

		// Act:
		network.updateLocalNodeEndpoint().join();

		// Assert:
		Assert.assertThat(connector.getNumGetLocalNodeInfoCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void updateLocalNodeEndpointForwardsValidParametersToGetLocalNodeInfo() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		network.refresh().join();

		// Act:
		network.updateLocalNodeEndpoint().join();

		// Assert:
		Assert.assertThat(
				connector.getLastGetLocalNodeInfoLocalEndpoint(),
				IsEqual.equalTo(network.getLocalNode().getEndpoint()));
	}

	@Test
	public void updateLocalNodeEndpointUpdatesLocalNodeEndpointWhenChanged() {
		// Assert:
		assertLocalNodeEndpointAfterUpdate(NodeEndpoint.fromHost("10.0.0.123"), NodeEndpoint.fromHost("10.0.0.123"));
	}

	@Test
	public void updateLocalNodeEndpointDoesNotUpdateLocalNodeEndpointWhenUnchanged() {
		// Assert:
		assertLocalNodeEndpointAfterUpdate(
				NodeEndpoint.fromHost(DEFAULT_LOCAL_NODE_HOST),
				NodeEndpoint.fromHost(DEFAULT_LOCAL_NODE_HOST));
	}

	@Test
	public void updateLocalNodeEndpointDoesNotUpdateLocalNodeEndpointWhenRemoteReturnsNull() {
		// Assert:
		assertLocalNodeEndpointAfterUpdate(null, NodeEndpoint.fromHost(DEFAULT_LOCAL_NODE_HOST));
	}

	private static void assertLocalNodeEndpointAfterUpdate(
			final NodeEndpoint endpointReturnedFromRemote,
			final NodeEndpoint expectedEndpoint) {
		// Arrange:
		final MockConnector connector = new MockConnector();
		connector.setGetLocalNodeInfoEndpoint(endpointReturnedFromRemote);

		final PeerNetwork network = createTestNetwork(connector);
		network.refresh().join();

		// Act:
		network.updateLocalNodeEndpoint().join();

		// Assert:
		Assert.assertThat(network.getLocalNode().getEndpoint(), IsEqual.equalTo(expectedEndpoint));
	}

	@Test
	public void updateLocalNodeEndpointDoesNotUpdateLocalNodeEndpointWhenRemoteFails() {
		// Assert:
		assertLocalNodeEndpointIsUnchangedOnFailure(MockConnector.TriggerAction.INACTIVE);
		assertLocalNodeEndpointIsUnchangedOnFailure(MockConnector.TriggerAction.FATAL);
	}

	private static void assertLocalNodeEndpointIsUnchangedOnFailure(final MockConnector.TriggerAction action) {
		// Arrange:
		final MockConnector connector = new MockConnector();
		connector.setGetLocalNodeInfoEndpoint(NodeEndpoint.fromHost("10.0.0.123"));
		for (final String host : new String[] { "10.0.0.1", "10.0.0.2", "10.0.0.3" })
			connector.setGetLocalNodeInfoError(host, action);

		final PeerNetwork network = createTestNetwork(connector);
		network.refresh().join();

		// Act:
		network.updateLocalNodeEndpoint().join();

		// Assert:
		Assert.assertThat(
				network.getLocalNode().getEndpoint(),
				IsEqual.equalTo(NodeEndpoint.fromHost(DEFAULT_LOCAL_NODE_HOST)));
	}

	//endregion

	//region updateExperience

	@Test
	public void updateExperienceUpdatesPartnerExperienceOnSuccess() {
		// Act:
		final NodeExperience experience = updateExperience("10.0.0.2", NodeInteractionResult.SUCCESS);

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(1L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(1L));
	}

	@Test
	public void updateExperienceUpdatesPartnerExperienceOnFailure() {
		// Act:
		final NodeExperience experience = updateExperience("10.0.0.2", NodeInteractionResult.FAILURE);

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(1L));
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(1L));
	}

	@Test
	public void updateExperienceDoesNotUpdatePartnerExperienceOnNeutral() {
		// Act:
		final NodeExperience experience = updateExperience("10.0.0.2", NodeInteractionResult.NEUTRAL);

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(0L));
	}

	@Test
	public void updateExperienceUpdatesUnknownNodeExperience() {
		// Act:
		final NodeExperience experience = updateExperience("10.0.0.25", NodeInteractionResult.SUCCESS);

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(1L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(1L));
	}

	@Test
	public void updateExperienceDoesNotUpdateLocalNodeExperience() {
		// Act:
		final NodeExperience experience = updateExperience(DEFAULT_LOCAL_NODE_HOST, NodeInteractionResult.SUCCESS);

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(0L));
	}

	private static NodeExperience updateExperience(final String host, final NodeInteractionResult result) {
		// Arrange:
		final NodeExperiences nodeExperiences = new NodeExperiences();
		final PeerNetwork network = createTestNetwork(nodeExperiences);
		final Node remoteNode = PeerUtils.createNodeWithHost(host);

		// Act:
		network.updateExperience(remoteNode, result);
		return nodeExperiences.getNodeExperience(network.getLocalNode(), remoteNode);
	}

	//endregion

	//region pruneInactiveNodes

	@Test
	public void pruneInactiveNodesDelegatesToNodeCollection() {
		// Arrange:
		final NodeCollection nodes = Mockito.mock(NodeCollection.class);
		final PeerNetwork network = createTestNetwork(nodes);

		// Act:
		network.pruneInactiveNodes();

		// Assert:
		Mockito.verify(nodes, Mockito.times(1)).pruneInactiveNodes();
	}

	//endregion

	//region threading

	@Test
	public void refreshIsAsync() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.SLEEP_INACTIVE);

		// Act:
		final CompletableFuture future = network.refresh();

		// Assert:
		Assert.assertThat(future.isDone(), IsEqual.equalTo(false));
	}

	@Test
	public void broadcastIsAsync() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		connector.setAnnounceDelay(true);
		final PeerNetwork network = createTestNetwork(connector);
		network.refresh().join(); // transition all nodes to active

		// Act:
		final CompletableFuture future = network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, new MockSerializableEntity());

		// Assert:
		Assert.assertThat(future.isDone(), IsEqual.equalTo(false));
	}

	//endregion

	//region getLocalNodeAndExperiences / setRemoteNodeExperiences

	@Test
	public void getLocalNodeAndExperiencesIncludesLocalNode() {
		// Arrange:
		final PeerNetwork network = createTestNetwork();

		// Act:
		final NodeExperiencesPair pair = network.getLocalNodeAndExperiences();

		// Assert:
		Assert.assertThat(pair.getNode(), IsSame.sameInstance(network.getLocalNode()));
	}

	@Test
	public void getLocalNodeAndExperiencesIncludesLocalNodeExperiences() {
		// Arrange:
		final NodeExperiences experiences = new NodeExperiences();
		final PeerNetwork network = createTestNetwork(experiences);
		final Node localNode = network.getLocalNode();
		final Node otherNode1 = PeerUtils.createNodeWithPort(91);
		final Node otherNode2 = PeerUtils.createNodeWithPort(97);

		experiences.getNodeExperience(localNode, otherNode1).successfulCalls().set(14);
		experiences.getNodeExperience(localNode, otherNode2).successfulCalls().set(7);

		// Act:
		final List<NodeExperiencePair> localNodeExperiences = network.getLocalNodeAndExperiences().getExperiences();

		NodeExperiencePair pair1 = localNodeExperiences.get(0);
		NodeExperiencePair pair2 = localNodeExperiences.get(1);
		if (pair1.getNode().equals(otherNode2)) {
			final NodeExperiencePair temp = pair1;
			pair1 = pair2;
			pair2 = temp;
		}

		// Assert:
		Assert.assertThat(pair1.getNode(), IsEqual.equalTo(otherNode1));
		Assert.assertThat(pair1.getExperience().successfulCalls().get(), IsEqual.equalTo(14L));
		Assert.assertThat(pair2.getNode(), IsEqual.equalTo(otherNode2));
		Assert.assertThat(pair2.getExperience().successfulCalls().get(), IsEqual.equalTo(7L));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotBatchSetLocalExperiences() {
		// Arrange:
		final NodeExperiences experiences = new NodeExperiences();
		final PeerNetwork network = createTestNetwork(experiences);
		final List<NodeExperiencePair> pairs = new ArrayList<>();
		pairs.add(new NodeExperiencePair(PeerUtils.createNodeWithPort(81), PeerUtils.createNodeExperience(14)));
		pairs.add(new NodeExperiencePair(PeerUtils.createNodeWithPort(83), PeerUtils.createNodeExperience(44)));

		// Act:
		network.setRemoteNodeExperiences(new NodeExperiencesPair(network.getLocalNode(), pairs));
	}

	@Test
	public void canBatchSetRemoteExperiences() {
		// Arrange:
		final NodeExperiences experiences = new NodeExperiences();
		final PeerNetwork network = createTestNetwork(experiences);

		final Node remoteNode = PeerUtils.createNodeWithPort(1);
		final Node otherNode1 = PeerUtils.createNodeWithPort(81);
		final Node otherNode2 = PeerUtils.createNodeWithPort(83);

		final List<NodeExperiencePair> pairs = Arrays.asList(
				new NodeExperiencePair(otherNode1, PeerUtils.createNodeExperience(14)),
				new NodeExperiencePair(otherNode2, PeerUtils.createNodeExperience(44)));

		// Act:
		network.setRemoteNodeExperiences(new NodeExperiencesPair(remoteNode, pairs));
		final NodeExperience experience1 = experiences.getNodeExperience(remoteNode, otherNode1);
		final NodeExperience experience2 = experiences.getNodeExperience(remoteNode, otherNode2);

		// Assert:
		Assert.assertThat(experience1.successfulCalls().get(), IsEqual.equalTo(14L));
		Assert.assertThat(experience2.successfulCalls().get(), IsEqual.equalTo(44L));
	}

	//endregion

	//region createWithVerificationOfLocalNode

	@Test
	public void createWithVerificationOfLocalNodeCallsGetLocalNodeInfoForAllActiveNodes() {
		// Arrange:
		final MockConnector connector = new MockConnector();

		// Act:
		createPeerNetworkWithVerificationOfLocalNode(connector).join();

		// Assert:
		Assert.assertThat(connector.getNumGetLocalNodeInfoCalls(), IsEqual.equalTo(3));
	}

	@Test
	public void createWithVerificationOfLocalNodePassesConfigEndpointToGetLocalNodeInfo() {
		// Arrange:
		final MockConnector connector = new MockConnector();

		// Act:
		createPeerNetworkWithVerificationOfLocalNode(connector).join();
		final NodeEndpoint lastLocalEndpoint = connector.getLastGetLocalNodeInfoLocalEndpoint();

		// Assert:
		assertNodeEndpointIsConfigLocalNodeEndpoint(lastLocalEndpoint);
	}

	@Test
	public void createWithVerificationOfLocalNodeUsesConfigEndpointIfNoCallSucceeds() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		connector.setGetLocalNodeInfoError("10.0.0.1", MockConnector.TriggerAction.INACTIVE);
		connector.setGetLocalNodeInfoError("10.0.0.2", MockConnector.TriggerAction.FATAL);
		connector.setGetLocalNodeInfoError("10.0.0.3", MockConnector.TriggerAction.INACTIVE);

		// Act:
		final PeerNetwork network = createPeerNetworkWithVerificationOfLocalNode(connector).join();

		// Assert:
		assertNodeIsConfigLocalNode(network.getLocalNode());
	}

	@Test
	public void createWithVerificationOfLocalNodeUsesConfigEndpointIfAllCallsReturnNull()  {
		// Arrange:
		final MockConnector connector = new MockConnector();

		// Act:
		final PeerNetwork network = createPeerNetworkWithVerificationOfLocalNode(connector).join();

		// Assert:
		assertNodeIsConfigLocalNode(network.getLocalNode());
	}

	@Test
	public void createWithVerificationOfLocalNodeUsesReportedEndpointIfAtLeastOneCallSucceeds() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		connector.setGetLocalNodeInfoError("10.0.0.1", MockConnector.TriggerAction.FATAL);
		connector.setGetLocalNodeInfoError("10.0.0.3", MockConnector.TriggerAction.FATAL);
		connector.setGetLocalNodeInfoEndpoint(new NodeEndpoint("http", "10.0.0.25", 8990)) ;

		// Act:
		final PeerNetwork network = createPeerNetworkWithVerificationOfLocalNode(connector).join();

		// Assert:
		final Node localNode = network.getLocalNode();
		final NodeMetaData localNodeMetaData = localNode.getMetaData();
		Assert.assertThat(localNode.getEndpoint(), IsEqual.equalTo(new NodeEndpoint("http", "10.0.0.25", 8990)));
		Assert.assertThat(localNodeMetaData.getPlatform(), IsEqual.equalTo("Mac"));
		Assert.assertThat(localNodeMetaData.getVersion(), IsEqual.equalTo("2.0"));
		Assert.assertThat(localNodeMetaData.getApplication(), IsEqual.equalTo("FooBar"));
	}

	@Test
	public void createWithVerificationOfLocalNodeIsAsync() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		connector.setGetLocalNodeInfoError("10.0.0.1", MockConnector.TriggerAction.SLEEP_INACTIVE);

		// Act:
		final CompletableFuture<PeerNetwork> networkCompletable = createPeerNetworkWithVerificationOfLocalNode(connector);

		// Assert:
		Assert.assertThat(networkCompletable.isDone(), IsEqual.equalTo(false));
	}

	private void assertNodeEndpointIsConfigLocalNodeEndpoint(final NodeEndpoint endpoint) {
		// Assert:
		Assert.assertThat(endpoint, IsEqual.equalTo(new NodeEndpoint("http", DEFAULT_LOCAL_NODE_HOST, 7890)));
	}

	private void assertNodeIsConfigLocalNode(final Node node) {
		// Assert:
		assertNodeEndpointIsConfigLocalNodeEndpoint(node.getEndpoint());
		Assert.assertThat(node.getMetaData().getPlatform(), IsEqual.equalTo("Mac"));
		Assert.assertThat(node.getMetaData().getApplication(), IsEqual.equalTo("FooBar"));
		Assert.assertThat(node.getMetaData().getVersion(), IsEqual.equalTo("2.0"));
	}

	public static CompletableFuture<PeerNetwork> createPeerNetworkWithVerificationOfLocalNode(final PeerConnector connector) {
		return PeerNetwork.createWithVerificationOfLocalNode(
				createTestConfig(),
				new PeerNetworkServices(
						connector,
						Mockito.mock(SyncConnectorPool.class),
						new MockBlockSynchronizer()));
	}

	//endregion

	//region test factories

	private static PeerNetworkServices createMockPeerNetworkServices() {
		return new PeerNetworkServices(
				new MockConnector(),
				Mockito.mock(SyncConnectorPool.class),
				new MockBlockSynchronizer());
	}

	private static PeerNetwork createTestNetwork(final MockBlockSynchronizer synchronizer) {
		return createTestNetwork(new MockConnector(), synchronizer);
	}

	private static PeerNetwork createTestNetwork(
			final PeerConnector connector,
			final MockBlockSynchronizer synchronizer) {
		return new PeerNetwork(
				createTestConfig(),
				new PeerNetworkServices(
						connector,
						Mockito.mock(SyncConnectorPool.class),
						synchronizer));
	}

	private static PeerNetwork createTestNetwork(final PeerConnector connector) {
		return createTestNetwork(connector, new MockBlockSynchronizer());
	}

	private static PeerNetwork createTestNetwork(final NodeExperiences nodeExperiences) {
		return new PeerNetwork(
				createTestConfig(),
				createMockPeerNetworkServices(),
				nodeExperiences,
				new NodeCollection());
	}

	private static PeerNetwork createTestNetwork(final NodeCollection nodes) {
		return new PeerNetwork(
				createTestConfig(),
				createMockPeerNetworkServices(),
				new NodeExperiences(),
				nodes);
	}

	private static PeerNetwork createTestNetwork(final TrustProvider provider) {
		return new PeerNetwork(
				createTestConfig(provider),
				createMockPeerNetworkServices());
	}

	private static PeerNetwork createTestNetwork() {
		return createTestNetwork(new MockConnector());
	}

	private static Config createTestConfig() {
		return createTestConfig(new EigenTrustPlusPlus());
	}

	private static Config createTestConfig(final TrustProvider provider) {
		final Node localNode = new Node(
				new WeakNodeIdentity(DEFAULT_LOCAL_NODE_HOST),
				NodeEndpoint.fromHost(DEFAULT_LOCAL_NODE_HOST),
				new NodeMetaData("Mac", "FooBar", "2.0"));

		final List<Node> wellKnownPeers = Arrays.asList(
				PeerUtils.createNodeWithHost("10.0.0.1"),
				PeerUtils.createNodeWithHost("10.0.0.3"),
				PeerUtils.createNodeWithHost("10.0.0.2"));

		final Config config = Mockito.mock(Config.class);
		Mockito.when(config.getTrustProvider()).thenReturn(provider);
		Mockito.when(config.getLocalNode()).thenReturn(localNode);
		Mockito.when(config.getPreTrustedNodes()).thenReturn(new PreTrustedNodes(new HashSet<>(wellKnownPeers)));
		Mockito.when(config.getTrustParameters()).thenReturn(new TrustParameters());
		return config;
	}

	//endregion
}
