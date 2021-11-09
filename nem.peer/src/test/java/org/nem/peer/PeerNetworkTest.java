package org.nem.peer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.core.time.synchronization.TimeSynchronizer;
import org.nem.peer.services.*;
import org.nem.peer.test.PeerUtils;
import org.nem.peer.trust.NodeSelector;
import org.nem.peer.trust.score.NodeExperiencesPair;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PeerNetworkTest {

	// region PeerNetworkState delegation

	@Test
	public void isChainSynchronizedDelegatesToState() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.network.isChainSynchronized();

		// Assert:
		Mockito.verify(context.state, Mockito.only()).isChainSynchronized();
	}

	@Test
	public void getLocalNodeDelegatesToState() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = NodeUtils.createNodeWithName("l");
		Mockito.when(context.network.getLocalNode()).thenReturn(localNode);

		// Act:
		final Node networkLocalNode = context.network.getLocalNode();

		// Assert:
		Mockito.verify(context.state, Mockito.only()).getLocalNode();
		MatcherAssert.assertThat(networkLocalNode, IsSame.sameInstance(localNode));
	}

	@Test
	public void getNodesDelegatesToState() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeCollection nodes = new NodeCollection();
		Mockito.when(context.network.getNodes()).thenReturn(nodes);

		// Act:
		final NodeCollection networkNodes = context.network.getNodes();

		// Assert:
		Mockito.verify(context.state, Mockito.only()).getNodes();
		MatcherAssert.assertThat(networkNodes, IsSame.sameInstance(nodes));
	}

	@Test
	public void getPartnerNodesDelegatesToSelector() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Node> nodes = Arrays.asList(NodeUtils.createNodeWithHost("10.0.0.4"), NodeUtils.createNodeWithHost("10.0.0.7"));
		Mockito.when(context.updateSelector.selectNodes()).thenReturn(nodes);

		// Act:
		final Collection<Node> selectedNodes = context.network.getPartnerNodes();

		// Assert:
		Mockito.verify(context.updateSelector, Mockito.only()).selectNodes();
		MatcherAssert.assertThat(selectedNodes, IsSame.sameInstance(nodes));
	}

	@Test
	public void getLocalNodeAndExperiencesDelegatesToState() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeExperiencesPair pair = new NodeExperiencesPair(NodeUtils.createNodeWithName("r"), new ArrayList<>());
		Mockito.when(context.network.getLocalNodeAndExperiences()).thenReturn(pair);

		// Act:
		final NodeExperiencesPair networkPair = context.network.getLocalNodeAndExperiences();

		// Assert:
		Mockito.verify(context.state, Mockito.only()).getLocalNodeAndExperiences();
		MatcherAssert.assertThat(networkPair, IsSame.sameInstance(pair));
	}

	@Test
	public void updateExperienceDelegatesToState() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node node = NodeUtils.createNodeWithName("r");

		// Act:
		context.network.updateExperience(node, NodeInteractionResult.SUCCESS);

		// Assert:
		Mockito.verify(context.state, Mockito.only()).updateExperience(node, NodeInteractionResult.SUCCESS);
	}

	@Test
	public void pruneNodeExperiencesDelegatesToState() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.network.pruneNodeExperiences(new TimeInstant(123));

		// Assert:
		Mockito.verify(context.state, Mockito.only()).pruneNodeExperiences(new TimeInstant(123));
	}

	// endregion

	// region PeerNetworkServicesFactory / NodeSelectorFactory delegation

	@Test
	public void constructorCreatesNodeSelector() {
		// Act:
		final TestContext context = new TestContext();

		// Assert:
		Mockito.verify(context.selectorFactory, Mockito.only()).createUpdateNodeSelector();
	}

	@Test
	public void refreshDelegatesToFactory() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeRefresher refresher = Mockito.mock(NodeRefresher.class);
		Mockito.when(refresher.refresh(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
		Mockito.when(context.servicesFactory.createNodeRefresher()).thenReturn(refresher);

		// Act:
		context.network.refresh().join();

		// Assert:
		Mockito.verify(context.servicesFactory, Mockito.only()).createNodeRefresher();
		Mockito.verify(refresher, Mockito.only()).refresh(context.refreshNodes);
	}

	@Test
	public void refreshRecreatesSelector() {
		// Arrange: set up the node refresher
		final TestContext context = new TestContext();
		final NodeRefresher refresher = Mockito.mock(NodeRefresher.class);
		Mockito.when(refresher.refresh(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
		Mockito.when(context.servicesFactory.createNodeRefresher()).thenReturn(refresher);

		// Arrange: change the selector returned by createUpdateNodeSelector
		final NodeSelector updateSelector = Mockito.mock(NodeSelector.class);
		final List<Node> nodes = Arrays.asList(NodeUtils.createNodeWithHost("10.0.0.4"), NodeUtils.createNodeWithHost("10.0.0.7"));
		Mockito.when(updateSelector.selectNodes()).thenReturn(nodes);
		Mockito.when(context.selectorFactory.createUpdateNodeSelector()).thenReturn(updateSelector);

		// Act: refresh and query the partner nodes (which should use the new selector)
		context.network.refresh().join();
		final Collection<Node> selectedNodes = context.network.getPartnerNodes();

		// Assert:
		Mockito.verify(context.selectorFactory, Mockito.times(2)).createUpdateNodeSelector(); // called once in construction and once in
																								// refresh
		Mockito.verify(updateSelector, Mockito.only()).selectNodes(); // called in getPartnerNodes
		MatcherAssert.assertThat(selectedNodes, IsSame.sameInstance(nodes));
	}

	@Test
	public void broadcastDelegatesToFactory() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeBroadcaster broadcaster = Mockito.mock(NodeBroadcaster.class);
		Mockito.when(broadcaster.broadcast(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(CompletableFuture.completedFuture(null));
		Mockito.when(context.servicesFactory.createNodeBroadcaster()).thenReturn(broadcaster);

		// Act:
		final SerializableEntity entity = new MockSerializableEntity();
		context.network.broadcast(NisPeerId.REST_PUSH_BLOCK, entity).join();

		// Assert:
		Mockito.verify(context.servicesFactory, Mockito.only()).createNodeBroadcaster();
		Mockito.verify(broadcaster, Mockito.only()).broadcast(context.updateNodes, NisPeerId.REST_PUSH_BLOCK, entity);
	}

	@Test
	public void synchronizeDelegatesToFactory() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeSynchronizer synchronizer = Mockito.mock(NodeSynchronizer.class);
		Mockito.when(context.servicesFactory.createNodeSynchronizer()).thenReturn(synchronizer);

		// Act:
		context.network.synchronize();

		// Assert:
		Mockito.verify(context.servicesFactory, Mockito.only()).createNodeSynchronizer();
		Mockito.verify(synchronizer, Mockito.only()).synchronize(context.updateSelector);
	}

	@Test
	public void updateNodeExperiencesDelegatesToFactory() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeExperiencesUpdater updater = Mockito.mock(NodeExperiencesUpdater.class);
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(context.servicesFactory.createNodeExperiencesUpdater(timeProvider)).thenReturn(updater);

		// Act:
		context.network.updateNodeExperiences(timeProvider);

		// Assert:
		Mockito.verify(context.servicesFactory, Mockito.only()).createNodeExperiencesUpdater(timeProvider);
		Mockito.verify(updater, Mockito.only()).update(context.updateSelector);
	}

	@Test
	public void pruneInactiveNodesDelegatesToFactory() {
		// Arrange:
		final TestContext context = new TestContext();
		final InactiveNodePruner pruner = Mockito.mock(InactiveNodePruner.class);
		Mockito.when(context.servicesFactory.createInactiveNodePruner()).thenReturn(pruner);

		// Act:
		context.network.pruneInactiveNodes();

		// Assert:
		Mockito.verify(context.servicesFactory, Mockito.only()).createInactiveNodePruner();
		Mockito.verify(pruner, Mockito.only()).prune(context.allNodes);
	}

	@Test
	public void updateLocalNodeEndpointDelegatesToFactory() {
		// Arrange:
		final TestContext context = new TestContext();
		final LocalNodeEndpointUpdater updater = Mockito.mock(LocalNodeEndpointUpdater.class);
		Mockito.when(updater.updatePlurality(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
		Mockito.when(context.servicesFactory.createLocalNodeEndpointUpdater()).thenReturn(updater);

		// Act:
		context.network.updateLocalNodeEndpoint().join();

		// Assert:
		Mockito.verify(context.servicesFactory, Mockito.only()).createLocalNodeEndpointUpdater();
		Mockito.verify(updater, Mockito.only()).updatePlurality(context.updateNodes);
	}

	@Test
	public void checkChainSynchronizationDelegatesToFactory() {
		// Arrange:
		final TestContext context = new TestContext();
		final ChainServices services = Mockito.mock(ChainServices.class);
		Mockito.when(context.servicesFactory.getChainServices()).thenReturn(services);
		Mockito.when(services.isChainSynchronized(Mockito.any())).thenReturn(CompletableFuture.completedFuture(true));

		// Act:
		context.network.checkChainSynchronization().join();

		// Assert:
		Mockito.verify(context.servicesFactory, Mockito.only()).getChainServices();
		Mockito.verify(services, Mockito.only()).isChainSynchronized(context.updateNodes);
	}

	@Test
	public void checkChainSynchronizationUpdatesNetworkState() {
		// Arrange:
		final TestContext context = new TestContext();
		final ChainServices services = Mockito.mock(ChainServices.class);
		Mockito.when(context.servicesFactory.getChainServices()).thenReturn(services);
		Mockito.when(services.isChainSynchronized(context.network.getPartnerNodes())).thenReturn(CompletableFuture.completedFuture(true));

		// Act:
		context.network.checkChainSynchronization().join();

		// Assert:
		Mockito.verify(context.state, Mockito.only()).setChainSynchronized(true);
	}

	@Test
	public void synchronizeTimeDelegatesToFactories() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeSynchronizer synchronizer = Mockito.mock(TimeSynchronizer.class);
		Mockito.when(synchronizer.synchronizeTime()).thenReturn(CompletableFuture.completedFuture(null));
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(context.servicesFactory.createTimeSynchronizer(context.timeSyncSelector, timeProvider)).thenReturn(synchronizer);

		// Act:
		context.network.synchronizeTime(timeProvider).join();

		// Assert:
		Mockito.verify(context.selectorFactory, Mockito.times(1)).createTimeSyncNodeSelector();
		Mockito.verify(context.servicesFactory, Mockito.only()).createTimeSynchronizer(context.timeSyncSelector, timeProvider);
		Mockito.verify(synchronizer, Mockito.only()).synchronizeTime();
	}

	@Test
	public void bootDelegatesToFactory() {
		// Arrange:
		final TestContext context = new TestContext();
		final LocalNodeEndpointUpdater updater = Mockito.mock(LocalNodeEndpointUpdater.class);
		Mockito.when(updater.updateAny(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
		Mockito.when(context.servicesFactory.createLocalNodeEndpointUpdater()).thenReturn(updater);

		// Act:
		context.network.boot().join();

		// Assert:
		Mockito.verify(context.servicesFactory, Mockito.only()).createLocalNodeEndpointUpdater();
		Mockito.verify(updater, Mockito.only()).updateAny(context.updateNodes);
	}

	// endregion

	private static class TestContext {
		private final PeerNetworkState state = Mockito.mock(PeerNetworkState.class);
		private final PeerNetworkServicesFactory servicesFactory = Mockito.mock(PeerNetworkServicesFactory.class);
		private final PeerNetworkNodeSelectorFactory selectorFactory = Mockito.mock(PeerNetworkNodeSelectorFactory.class);
		private final NodeSelector refreshSelector = Mockito.mock(NodeSelector.class);
		private final NodeSelector updateSelector = Mockito.mock(NodeSelector.class);
		private final NodeSelector timeSyncSelector = Mockito.mock(NodeSelector.class);
		private final PeerNetwork network;

		private final List<Node> refreshNodes = PeerUtils.createNodesWithNames("r1", "r2");
		private final List<Node> updateNodes = PeerUtils.createNodesWithNames("u1", "u2");
		private final List<Node> timeSyncNodes = PeerUtils.createNodesWithNames("t1", "t2");
		private final NodeCollection allNodes = new NodeCollection();

		public TestContext() {
			for (final Node node : this.refreshNodes) {
				this.allNodes.update(node, NodeStatus.ACTIVE);
			}

			for (final Node node : this.updateNodes) {
				this.allNodes.update(node, NodeStatus.ACTIVE);
			}

			for (final Node node : this.timeSyncNodes) {
				this.allNodes.update(node, NodeStatus.ACTIVE);
			}

			Mockito.when(this.refreshSelector.selectNodes()).thenReturn(this.refreshNodes);
			Mockito.when(this.updateSelector.selectNodes()).thenReturn(this.updateNodes);
			Mockito.when(this.timeSyncSelector.selectNodes()).thenReturn(this.timeSyncNodes);
			Mockito.when(this.state.getNodes()).thenReturn(this.allNodes);

			Mockito.when(this.selectorFactory.createRefreshNodeSelector()).thenReturn(this.refreshSelector);
			Mockito.when(this.selectorFactory.createUpdateNodeSelector()).thenReturn(this.updateSelector);
			Mockito.when(this.selectorFactory.createTimeSyncNodeSelector()).thenReturn(this.timeSyncSelector);

			this.network = new PeerNetwork(this.state, this.servicesFactory, this.selectorFactory);
		}
	}
}
