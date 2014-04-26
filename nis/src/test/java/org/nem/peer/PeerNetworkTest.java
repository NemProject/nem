package org.nem.peer;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.test.*;
import org.nem.peer.node.*;
import org.nem.peer.scheduling.Scheduler;
import org.nem.peer.scheduling.SchedulerFactory;
import org.nem.peer.test.Utils;
import org.nem.peer.test.*;
import org.nem.peer.trust.score.*;

import java.util.*;

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
		network.refresh();

		// Assert:
		Assert.assertThat(connector.getNumGetInfoCalls(), IsEqual.equalTo(3));
	}

	@Test
	public void refreshCallsGetInfoForEveryActiveNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
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
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.2", MockConnector.TriggerAction.NONE);

		// Act:
		network.refresh();
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
		network.refresh();
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
		network.refresh();
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
		network.refresh();
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
		network.refresh();

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
		network.refresh();

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
		network.refresh();

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
		network.refresh();

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
		network.refresh();
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
		network.refresh();
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
		network.refresh();
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
		network.refresh();
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
		network.refresh();
		final NodeCollection nodes = network.getNodes();

		// Assert:
		NodeCollectionAssert.areHostsEquivalent(
				nodes,
				new String[] { "10.0.0.1", "10.0.0.2", "10.0.0.3" },
				new String[] { });
	}

	//endregion

	//endregion

	//region getPartnerNode

	@Test
	public void getPartnerNodeReturnsActiveNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		connector.setGetInfoError("10.0.0.1", MockConnector.TriggerAction.INACTIVE);
		connector.setGetInfoError("10.0.0.3", MockConnector.TriggerAction.FATAL);

		network.refresh();

		// Act:
		final NodeExperiencePair pair = network.getPartnerNode();

		// Assert:
		Assert.assertThat(pair.getNode().getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo("10.0.0.2"));
	}

	//endregion

	//region broadcast

	@Test
	public void broadcastDoesNotCallAnnounceForAnyInactiveNode() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);

		// Act:
		network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, new MockSerializableEntity());

		// Assert:
		Assert.assertThat(connector.getNumAnnounceCalls(), IsEqual.equalTo(0));
	}

	@Test
	public void broadcastCallsAnnounceForAllActiveNodes() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		network.refresh(); // transition all nodes to active

		// Act:
		network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, new MockSerializableEntity());

		// Assert:
		Assert.assertThat(connector.getNumAnnounceCalls(), IsEqual.equalTo(3));
	}

	@Test
	public void broadcastForwardsParametersToAnnounce() {
		// Arrange:
		final MockConnector connector = new MockConnector();
		final PeerNetwork network = createTestNetwork(connector);
		final SerializableEntity entity = new MockSerializableEntity();
		network.refresh(); // transition all nodes to active

		// Act:
		network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, entity);

		// Assert:
		Assert.assertThat(connector.getLastAnnounceId(), IsEqual.equalTo(NodeApiId.REST_PUSH_TRANSACTION));
		Assert.assertThat(connector.getLastAnnounceEntity(), IsEqual.equalTo(entity));
	}

	//endregion

	//region synchronize

	@Test
	public void synchronizeDoesNotCallSynchronizeNodeForAnyInactiveNode() {
		// Arrange:
		final MockBlockSynchronizer synchronizer = new MockBlockSynchronizer();
		final PeerNetwork network = createTestNetwork(synchronizer);

		// Act:
		network.synchronize();

		// Assert:
		Assert.assertThat(synchronizer.getNumSynchronizeNodeCalls(), IsEqual.equalTo(0));
	}

	@Test
	public void synchronizeCallsSynchronizeNodeForAllActiveNodes() {
		// Arrange:
		final MockBlockSynchronizer synchronizer = new MockBlockSynchronizer();
		final PeerNetwork network = createTestNetwork(synchronizer);
		network.refresh(); // transition all nodes to active

		// Act:
		network.synchronize();

		// Assert:
		Assert.assertThat(synchronizer.getNumSynchronizeNodeCalls(), IsEqual.equalTo(3));
	}

	@Test
	public void synchronizeForwardsValidParametersToSynchronizeNode() {
		// Arrange:
		final MockBlockSynchronizer synchronizer = new MockBlockSynchronizer();
		final PeerNetwork network = createTestNetwork(synchronizer);
		network.refresh(); // transition all nodes to active

		// Act:
		network.synchronize();

		// Assert:
		Assert.assertThat(synchronizer.getLastConnectorPool(), IsNot.not(IsEqual.equalTo(null)));
	}

	//endregion

	//region threading

	// TODO: fix this test
//	@Test
//	public void broadcastAndRefreshCanBeAccessedConcurrently() throws Exception {
//
//		class TestRunner {
//
//			final MockConnector connector = new MockConnector();
//
//			// configure a MockScheduler to be returned by the second (broadcast) createScheduler request
//			// (the first request is for the network call that initially makes everything active)
//			final SchedulerFactory<Node> schedulerFactory = new MockNodeSchedulerFactory(new MockScheduler(), 1);
//			final PeerNetwork network = new PeerNetwork(
//					createTestConfig(),
//					new PeerNetworkServices(
//							this.connector,
//							Mockito.mock(SyncConnectorPool.class),
//							this.schedulerFactory,
//							new MockBlockSynchronizer()));
//			final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
//
//			// monitor that is signaled when MockScheduler.push is entered
//			final Object schedulerPartialIterationMonitor = new Object();
//
//			// monitor that is signaled when the network refresh operation is complete
//			final Object networkRefreshCompleteMonitor = new Object();
//
//			public TestRunner() {
//				// Arrange: mark all nodes as active
//				this.network.refresh();
//
//				// Arrange: configure the next network call to return new nodes (so the connector needs to be updated)
//				NodeCollection knownPeers = new NodeCollection();
//				knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.15", 12), "p", "a"), NodeStatus.ACTIVE);
//				knownPeers.update(new Node(new NodeEndpoint("ftp", "10.0.0.7", 12), "p", "a"), NodeStatus.INACTIVE);
//				connector.setKnownPeers(knownPeers);
//			}
//
//			public List<Throwable> getExceptions() {
//				return this.exceptions;
//			}
//
//			public void run() throws InterruptedException {
//				// Act: trigger broadcast operation on a different thread
//				Thread broadcastThread = startThread(() ->
//						network.broadcast(
//						        NodeApiId.REST_PUSH_TRANSACTION,
//						        new MockTransaction(org.nem.core.test.Utils.generateRandomAccount())));
//
//				// Act: wait for the scheduler to partially iterate the collection
//				org.nem.core.test.Utils.monitorWait(this.schedulerPartialIterationMonitor);
//
//				// Act: trigger refresh on a different thread
//				Thread refreshThread = startThread(network::refresh);
//
//				// Act: wait for the refresh to complete
//				refreshThread.join();
//
//				// Act: signal the broadcast thread and let it complete
//				org.nem.core.test.Utils.monitorSignal(this.networkRefreshCompleteMonitor);
//				broadcastThread.join();
//			}
//
//			Thread startThread(final Runnable runnable) {
//				Thread t = new Thread(runnable);
//				t.setUncaughtExceptionHandler(new UncaughtExceptionHandler());
//				t.start();
//				return t;
//			}
//
//			class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
//				@Override
//				public void uncaughtException(Thread t, Throwable e) {
//					exceptions.add(e);
//				}
//			}
//
//			class MockScheduler implements Scheduler<Node> {
//				@Override
//				public void push(final Collection<Node> elements) {
//					// Act: Perform a partial iteration and move to the first element
//					final Iterator<Node> it = elements.iterator();
//					it.next();
//
//					// Arrange: unblock the main thread since the mock scheduler has been created and used
//					org.nem.core.test.Utils.monitorSignal(schedulerPartialIterationMonitor);
//
//					// Act:
//					org.nem.core.test.Utils.monitorWait(networkRefreshCompleteMonitor);
//
//					// Act: move to the next element
//					it.next();
//				}
//
//				@Override
//				public void block() {
//				}
//			}
//		}
//
//		// Arrange:
//		final TestRunner runner = new TestRunner();
//
//		// Act:
//		runner.run();
//
//		// Assert:
//		Assert.assertThat(0, IsEqual.equalTo(runner.getExceptions().size()));
//	}

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
				new MockNodeSchedulerFactory(),
				new MockBlockSynchronizer());
	}

	private static PeerNetwork createTestNetwork(final MockBlockSynchronizer synchronizer) {
		return new PeerNetwork(
				ConfigFactory.createDefaultTestConfig(),
				new PeerNetworkServices(
						new MockConnector(),
						Mockito.mock(SyncConnectorPool.class),
						new MockNodeSchedulerFactory(),
						synchronizer));
	}

	private static PeerNetwork createTestNetwork(final MockConnector connector) {
		return new PeerNetwork(
				ConfigFactory.createDefaultTestConfig(),
				new PeerNetworkServices(
						connector,
						Mockito.mock(SyncConnectorPool.class),
						new MockNodeSchedulerFactory(),
						new MockBlockSynchronizer()));
	}

	private static PeerNetwork createTestNetwork(final NodeExperiences nodeExperiences) {
		return new PeerNetwork(
				ConfigFactory.createDefaultTestConfig(),
				new PeerNetworkServices(
						new MockConnector(),
						Mockito.mock(SyncConnectorPool.class),
						new MockNodeSchedulerFactory(),
						new MockBlockSynchronizer()),
				nodeExperiences);
	}

	private static PeerNetwork createTestNetwork() {
		return createTestNetwork(new MockConnector());
	}

	private static Config createTestConfig() {
		return ConfigFactory.createDefaultTestConfig();
	}

	//endregion
}
