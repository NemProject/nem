package org.nem.nis.connect;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.time.SystemTimeProvider;
import org.nem.nis.time.synchronization.*;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.node.NodeCompatibilityChecker;
import org.nem.peer.services.*;

public class DefaultPeerNetworkServicesFactoryTest {

	@Test
	public void createInactiveNodePrunerReturnsNonNull() {
		// Assert:
		MatcherAssert.assertThat(createFactory().createInactiveNodePruner(), IsNull.notNullValue());
	}

	@Test
	public void createLocalNodeEndpointUpdaterReturnsNonNull() {
		// Assert:
		MatcherAssert.assertThat(createFactory().createLocalNodeEndpointUpdater(), IsNull.notNullValue());
	}

	@Test
	public void createNodeBroadcasterReturnsNonNull() {
		// Assert:
		MatcherAssert.assertThat(createFactory().createNodeBroadcaster(), IsNull.notNullValue());
	}

	@Test
	public void createNodeRefresherReturnsNonNull() {
		// Assert:
		MatcherAssert.assertThat(createFactory().createNodeRefresher(), IsNull.notNullValue());
	}

	@Test
	public void createNodeSynchronizerReturnsNonNull() {
		// Assert:
		MatcherAssert.assertThat(createFactory().createNodeSynchronizer(), IsNull.notNullValue());
	}

	@Test
	public void createNodeExperienceUpdaterReturnsNonNull() {
		// Assert:
		MatcherAssert.assertThat(createFactory().createNodeExperiencesUpdater(Mockito.mock(SystemTimeProvider.class)),
				IsNull.notNullValue());
	}

	@Test
	public void getChainServicesReturnsNonNull() {
		// Assert:
		MatcherAssert.assertThat(createFactory().getChainServices(), IsNull.notNullValue());
	}

	@Test
	public void createTimeSynchronizerReturnsNonNull() {
		// Assert:
		MatcherAssert.assertThat(createFactory().createTimeSynchronizer(Mockito.mock(ImportanceAwareNodeSelector.class),
				Mockito.mock(SystemTimeProvider.class)), IsNull.notNullValue());
	}

	private static PeerNetworkServicesFactory createFactory() {
		return new DefaultPeerNetworkServicesFactory(Mockito.mock(PeerNetworkState.class), Mockito.mock(PeerConnector.class),
				Mockito.mock(TimeSynchronizationConnector.class), Mockito.mock(SyncConnectorPool.class),
				Mockito.mock(BlockSynchronizer.class), Mockito.mock(ChainServices.class), Mockito.mock(TimeSynchronizationStrategy.class),
				Mockito.mock(NodeCompatibilityChecker.class));
	}
}
