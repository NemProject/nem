package org.nem.peer;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.node.*;
import org.nem.core.test.MockSerializableEntity;
import org.nem.core.time.TimeProvider;
import org.nem.core.time.synchronization.TimeSynchronizer;
import org.nem.nis.service.ChainServices;
import org.nem.nis.time.synchronization.ImportanceAwareNodeSelector;
import org.nem.peer.services.*;
import org.nem.peer.test.PeerUtils;
import org.nem.peer.trust.NodeSelector;
import org.nem.peer.trust.score.NodeExperiencesPair;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class PeerNetworkTest {

	//region PeerNetworkState delegation

	@Test
	public void isChainSynchronizedDelegatesToState() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.network.isChainSynchronized();

		// Assert:
		Mockito.verify(context.state, Mockito.times(1)).isChainSynchronized();
	}

	@Test
	public void getLocalNodeDelegatesToState() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = PeerUtils.createNodeWithName("l");
		Mockito.when(context.network.getLocalNode()).thenReturn(localNode);

		// Act:
		final Node networkLocalNode = context.network.getLocalNode();

		// Assert:
		Mockito.verify(context.state, Mockito.times(1)).getLocalNode();
		Assert.assertThat(networkLocalNode, IsSame.sameInstance(localNode));
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
		Mockito.verify(context.state, Mockito.times(1)).getNodes();
		Assert.assertThat(networkNodes, IsSame.sameInstance(nodes));
	}

	@Test
	public void getLocalNodeAndExperiencesDelegatesToState() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeExperiencesPair pair = new NodeExperiencesPair(PeerUtils.createNodeWithName("r"), new ArrayList<>());
		Mockito.when(context.network.getLocalNodeAndExperiences()).thenReturn(pair);

		// Act:
		final NodeExperiencesPair networkPair = context.network.getLocalNodeAndExperiences();

		// Assert:
		Mockito.verify(context.state, Mockito.times(1)).getLocalNodeAndExperiences();
		Assert.assertThat(networkPair, IsSame.sameInstance(pair));
	}

	@Test
	public void updateExperienceDelegatesToState() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node node = PeerUtils.createNodeWithName("r");

		// Act:
		context.network.updateExperience(node, NodeInteractionResult.SUCCESS);

		// Assert:
		Mockito.verify(context.state, Mockito.times(1)).updateExperience(node, NodeInteractionResult.SUCCESS);
	}

	@Test
	public void setRemoteNodeExperiencesDelegatesToState() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeExperiencesPair pair = new NodeExperiencesPair(PeerUtils.createNodeWithName("r"), new ArrayList<>());

		// Act:
		context.network.setRemoteNodeExperiences(pair);

		// Assert:
		Mockito.verify(context.state, Mockito.times(1)).setRemoteNodeExperiences(pair);
	}

	//endregion

	//region PeerNetworkServicesFactory / NodeSelectorFactory delegation

	@Test
	public void constructorCreatesNodeSelector() {
		// Act:
		final TestContext context = new TestContext();

		// Assert:
		Mockito.verify(context.selectorFactory, Mockito.times(1)).createNodeSelector();
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
		Mockito.verify(context.servicesFactory, Mockito.times(1)).createNodeRefresher();
		Mockito.verify(refresher, Mockito.times(1)).refresh(Mockito.any());
	}

	@Test
	public void refreshRecreatesSelector() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeRefresher refresher = Mockito.mock(NodeRefresher.class);
		Mockito.when(refresher.refresh(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
		Mockito.when(context.servicesFactory.createNodeRefresher()).thenReturn(refresher);

		// Act:
		context.network.refresh().join();

		// Assert:
		Mockito.verify(context.selectorFactory, Mockito.times(2)).createNodeSelector();
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
		context.network.broadcast(NodeApiId.REST_PUSH_BLOCK, new MockSerializableEntity()).join();

		// Assert:
		Mockito.verify(context.servicesFactory, Mockito.times(1)).createNodeBroadcaster();
		Mockito.verify(broadcaster, Mockito.times(1)).broadcast(Mockito.any(), Mockito.any(), Mockito.any());
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
		Mockito.verify(context.servicesFactory, Mockito.times(1)).createNodeSynchronizer();
		Mockito.verify(synchronizer, Mockito.times(1)).synchronize(Mockito.any());
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
		Mockito.verify(context.servicesFactory, Mockito.times(1)).createInactiveNodePruner();
		Mockito.verify(pruner, Mockito.times(1)).prune(Mockito.any());
	}

	@Test
	public void updateLocalNodeEndpointDelegatesToFactory() {
		// Arrange:
		final TestContext context = new TestContext();
		final LocalNodeEndpointUpdater updater = Mockito.mock(LocalNodeEndpointUpdater.class);
		Mockito.when(updater.update(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
		Mockito.when(context.servicesFactory.createLocalNodeEndpointUpdater()).thenReturn(updater);

		// Act:
		context.network.updateLocalNodeEndpoint().join();

		// Assert:
		Mockito.verify(context.servicesFactory, Mockito.times(1)).createLocalNodeEndpointUpdater();
		Mockito.verify(updater, Mockito.times(1)).update(Mockito.any());
	}

	@Test
	public void checkChainSynchronizationDelegatesToFactory() {
		// Arrange:
		final TestContext context = new TestContext();
		final ChainServices services = Mockito.mock(ChainServices.class);
		Mockito.when(context.servicesFactory.getChainServices()).thenReturn(services);

		// Act:
		context.network.checkChainSynchronization();

		// Assert:
		Mockito.verify(context.servicesFactory, Mockito.times(1)).getChainServices();
		Mockito.verify(services, Mockito.times(1)).isChainSynchronized(Mockito.any());
	}

	@Test
	public void checkChainSynchronizationUpdatesNetworkState() {
		// Arrange:
		final TestContext context = new TestContext();
		final ChainServices services = Mockito.mock(ChainServices.class);
		final Node node = PeerUtils.createNodeWithName("r");
		Mockito.when(services.isChainSynchronized(node)).thenReturn(true);
		Mockito.when(context.servicesFactory.getChainServices()).thenReturn(services);
		Mockito.when(context.state.getLocalNode()).thenReturn(node);

		// Act:
		context.network.checkChainSynchronization();

		// Assert:
		Mockito.verify(context.state, Mockito.times(1)).setChainSynchronized(true);
	}

	@Test
	public void synchronizeTimeDelegatesToFactories() {
		// Arrange:
		final TestContext context = new TestContext();
		final TimeSynchronizer synchronizer = Mockito.mock(TimeSynchronizer.class);
		Mockito.when(synchronizer.synchronizeTime()).thenReturn(CompletableFuture.completedFuture(null));
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		final ImportanceAwareNodeSelector nodeSelector = Mockito.mock(ImportanceAwareNodeSelector.class);
		Mockito.when(context.importanceAwareSelectorFactory.createNodeSelector()).thenReturn(nodeSelector);
		Mockito.when(context.servicesFactory.createTimeSynchronizer(nodeSelector, timeProvider)).thenReturn(synchronizer);

		// Act:
		context.network.synchronizeTime(timeProvider).join();

		// Assert:
		Mockito.verify(context.importanceAwareSelectorFactory, Mockito.times(1)).createNodeSelector();
		Mockito.verify(context.servicesFactory, Mockito.times(1)).createTimeSynchronizer(nodeSelector, timeProvider);
		Mockito.verify(synchronizer, Mockito.times(1)).synchronizeTime();
	}

	//endregion

	private static class TestContext {
		private final PeerNetworkState state = Mockito.mock(PeerNetworkState.class);
		private final PeerNetworkServicesFactory servicesFactory = Mockito.mock(PeerNetworkServicesFactory.class);
		private final NodeSelectorFactory selectorFactory = Mockito.mock(NodeSelectorFactory.class);
		private final NodeSelectorFactory importanceAwareSelectorFactory = Mockito.mock(NodeSelectorFactory.class);
		private final PeerNetwork network;

		public TestContext() {
			final NodeSelector selector = Mockito.mock(NodeSelector.class);
			Mockito.when(selector.selectNodes()).thenReturn(new ArrayList<>());
			Mockito.when(this.selectorFactory.createNodeSelector()).thenReturn(selector);

			this.network = new PeerNetwork(this.state, this.servicesFactory, this.selectorFactory, importanceAwareSelectorFactory);
		}
	}
}
