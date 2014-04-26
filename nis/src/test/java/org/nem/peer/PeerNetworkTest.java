package org.nem.peer;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.test.*;
import org.nem.peer.node.*;
import org.nem.peer.test.Utils;
import org.nem.peer.test.*;
import org.nem.peer.trust.TrustProvider;
import org.nem.peer.trust.score.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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
		final MockConnector connector = new MockConnector();
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

	@Test
	public void refreshCallsGetInfoForEveryInactiveNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);

		// Act:
		network.refresh().join();

		// Assert:
		Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(3));
	}

	@Test
	public void refreshCallsGetInfoForEveryActiveNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);

		// Act:
		network.refresh().join(); // transition all nodes to active
		network.refresh().join();

		// Assert:
		Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(6));
	}

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
	public void refreshNodeChangeAddressRemovesNodesFromBothLists() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.CHANGE_ADDRESS);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(nodes, new String[] { "10.0.0.1", "10.0.0.3" }, new String[] { });
	}

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
	public void refreshDoesNotCallGetKnownPeersForChangeAddressNodes() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.CHANGE_ADDRESS);

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

		// Arrange: set up a node peers list that indicates the reverse of direct communication
		// (i.e. 10.0.0.2 is active and all other nodes are inactive)
		final NodeCollection knownPeers = new NodeCollection();
		knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.1", 12), "p", "a"), NodeStatus.INACTIVE);
		knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.2", 12), "p", "a"), NodeStatus.ACTIVE);
		knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.3", 12), "p", "a"), NodeStatus.INACTIVE);
		connector.setKnownPeers(knownPeers);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(nodes, new String[] { "10.0.0.1", "10.0.0.3" }, new String[] { "10.0.0.2" });
	}

	@Test
	public void refreshMergesInKnownPeers() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);

		final NodeCollection knownPeers = new NodeCollection();
		knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.15", 12), "p", "a"), NodeStatus.ACTIVE);
		knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.7", 12), "p", "a"), NodeStatus.INACTIVE);
		knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.11", 12), "p", "a"), NodeStatus.INACTIVE);
		knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.8", 12), "p", "a"), NodeStatus.ACTIVE);
		connector.setKnownPeers(knownPeers);

		// Act:
		network.refresh().join();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(
				nodes,
				new String[] { "10.0.0.1", "10.0.0.2", "10.0.0.3", "10.0.0.8", "10.0.0.15" },
				new String[] { "10.0.0.7", "10.0.0.11" });
	}

	@Test
	public void refreshDoesNotMergeInLocalNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);

		final NodeCollection knownPeers = new NodeCollection();
		knownPeers.update(network.getLocalNode(), NodeStatus.ACTIVE);
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
		connector.setGetInfoError("10.0.0.1", MockConnector.TriggerAction.INACTIVE);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.INACTIVE);
		connector.setGetInfoError("10.0.0.3", MockConnector.TriggerAction.INACTIVE);
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
		Assert.assertThat(synchronizer.getLastConnectorPool(), IsNot.not(IsEqual.equalTo(null)));
	}

	@Test
	public void synchronizeSyncsWithActiveNode() {
		// Arrange:
		final MockBlockSynchronizer synchronizer = new MockBlockSynchronizer();
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector, synchronizer);
		connector.setGetInfoError("10.0.0.1", MockConnector.TriggerAction.INACTIVE);
		connector.setGetInfoError("10.0.0.3", MockConnector.TriggerAction.FATAL);

		network.refresh().join();

		// Act:
		network.synchronize();

		// Assert:
		Assert.assertThat(
				synchronizer.getLastNode().getEndpoint().getBaseUrl().getHost(),
				IsEqual.equalTo("10.0.0.2"));
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
		final Node otherNode1 = Utils.createNodeWithPort(91);
		final Node otherNode2 = Utils.createNodeWithPort(97);

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
		pairs.add(new NodeExperiencePair(Utils.createNodeWithPort(81), Utils.createNodeExperience(14)));
		pairs.add(new NodeExperiencePair(Utils.createNodeWithPort(83), Utils.createNodeExperience(44)));

		// Act:
		network.setRemoteNodeExperiences(new NodeExperiencesPair(network.getLocalNode(), pairs));
	}

	@Test
	public void canBatchSetRemoteExperiences() {
		// Arrange:
		final NodeExperiences experiences = new NodeExperiences();
		final PeerNetwork network = createTestNetwork(experiences);

		final Node remoteNode = Utils.createNodeWithPort(1);
		final Node otherNode1 = Utils.createNodeWithPort(81);
		final Node otherNode2 = Utils.createNodeWithPort(83);

		final List<NodeExperiencePair> pairs = new ArrayList<>();
		pairs.add(new NodeExperiencePair(otherNode1, Utils.createNodeExperience(14)));
		pairs.add(new NodeExperiencePair(otherNode2, Utils.createNodeExperience(44)));

		// Act:
		network.setRemoteNodeExperiences(new NodeExperiencesPair(remoteNode, pairs));
		final NodeExperience experience1 = experiences.getNodeExperience(remoteNode, otherNode1);
		final NodeExperience experience2 = experiences.getNodeExperience(remoteNode, otherNode2);

		// Assert:
		Assert.assertThat(experience1.successfulCalls().get(), IsEqual.equalTo(14L));
		Assert.assertThat(experience2.successfulCalls().get(), IsEqual.equalTo(44L));
	}

	//endregion

	//region factories

	private static PeerNetworkServices createMockPeerNetworkServices() {
		return new PeerNetworkServices(
				new MockConnector(),
				Mockito.mock(SyncConnectorPool.class),
				new MockBlockSynchronizer());
	}

	private static PeerNetwork createTestNetwork(final MockBlockSynchronizer synchronizer) {
		return createTestNetwork(new MockConnector(), synchronizer);
	}

	private static PeerNetwork createTestNetwork(final MockConnector connector, final MockBlockSynchronizer synchronizer) {
		return new PeerNetwork(
				ConfigFactory.createDefaultTestConfig(),
				new PeerNetworkServices(
						connector,
						Mockito.mock(SyncConnectorPool.class),
						synchronizer));
	}

	private static PeerNetwork createTestNetwork(final MockConnector connector) {
		return createTestNetwork(connector, new MockBlockSynchronizer());
	}

	private static PeerNetwork createTestNetwork(final NodeExperiences nodeExperiences) {
		return new PeerNetwork(
				ConfigFactory.createDefaultTestConfig(),
				new PeerNetworkServices(
						new MockConnector(),
						Mockito.mock(SyncConnectorPool.class),
						new MockBlockSynchronizer()),
				nodeExperiences);
	}

	private static PeerNetwork createTestNetwork(final TrustProvider provider) {
		return new PeerNetwork(
				ConfigFactory.createConfig(provider),
				new PeerNetworkServices(
						new MockConnector(),
						Mockito.mock(SyncConnectorPool.class),
						new MockBlockSynchronizer()));
	}

	private static PeerNetwork createTestNetwork() {
		return createTestNetwork(new MockConnector());
	}

	private static Config createTestConfig() {
		return ConfigFactory.createDefaultTestConfig();
	}

	//endregion
}
