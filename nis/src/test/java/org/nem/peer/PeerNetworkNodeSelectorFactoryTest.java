package org.nem.peer;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.*;
import org.nem.core.test.NodeUtils;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountState;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.NodeExperiences;

import java.util.*;
import java.util.function.Function;

public class PeerNetworkNodeSelectorFactoryTest {

	//region createRefreshNodeSelector

	@Test
	public void createRefreshNodeSelectorReturnsNonNull() {
		// Assert:
		assertNonNullSelector(PeerNetworkNodeSelectorFactory::createRefreshNodeSelector);
	}

	@Test
	public void refreshNodeSelectorCanReturnBusyNodes() {
		// Assert:
		assertNumSelectedNodes(PeerNetworkNodeSelectorFactory::createRefreshNodeSelector, 2);
	}

	//endregion

	//region createUpdateNodeSelector

	@Test
	public void createUpdateNodeSelectorReturnsNonNull() {
		// Assert:
		assertNonNullSelector(PeerNetworkNodeSelectorFactory::createUpdateNodeSelector);
	}

	@Test
	public void updateNodeSelectorCannotReturnBusyNodes() {
		// Assert:
		assertNumSelectedNodes(PeerNetworkNodeSelectorFactory::createUpdateNodeSelector, 1);
	}

	//endregion

	//region createTimeSyncNodeSelector

	@Test
	public void createTimeSyncNodeSelectorReturnsNonNull() {
		// Assert:
		assertNonNullSelector(PeerNetworkNodeSelectorFactory::createTimeSyncNodeSelector);
	}

	@Test
	public void timeSyncNodeSelectorCannotReturnBusyNodes() {
		// Assert:
		assertNumSelectedNodes(PeerNetworkNodeSelectorFactory::createTimeSyncNodeSelector, 1);
	}

	//endregion

	private static void assertNonNullSelector(final Function<PeerNetworkNodeSelectorFactory, NodeSelector> createSelector) {
		// Arrange:
		final PeerNetworkNodeSelectorFactory factory = createFactory();

		// Act:
		final NodeSelector selector = createSelector.apply(factory);

		// Assert:
		Assert.assertThat(selector, IsNull.notNullValue());
	}

	private static void assertNumSelectedNodes(final Function<PeerNetworkNodeSelectorFactory, NodeSelector> createSelector, final int expectedNumNodes) {
		// Arrange:
		final PeerNetworkNodeSelectorFactory factory = createFactory();
		final NodeSelector selector = createSelector.apply(factory);

		// Act:
		final Collection<Node> nodes = selector.selectNodes();

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(expectedNumNodes));
	}

	private static PeerNetworkNodeSelectorFactory createFactory() {
		final Config config = createConfig();
		final NodeCollection nodes = new NodeCollection();
		nodes.update(NodeUtils.createNodeWithName("a"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("b"), NodeStatus.BUSY);

		final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		Mockito.when(poiFacade.getLastPoiRecalculationHeight()).thenReturn(BlockHeight.ONE);

		final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		Mockito.when(accountStateCache.findStateByAddress(Mockito.any())).thenAnswer(invocationOnMock -> {
			final AccountState accountState = new AccountState((Address)invocationOnMock.getArguments()[0]);
			accountState.getImportanceInfo().setImportance(BlockHeight.ONE, 0.75);
			return accountState;
		});

		return new PeerNetworkNodeSelectorFactory(
				createNisConfiguration(),
				config.getTrustProvider(),
				new PeerNetworkState(config, new NodeExperiences(), nodes),
				poiFacade,
				accountStateCache);
	}

	private static NisConfiguration createNisConfiguration() {
		final NisConfiguration config = Mockito.mock(NisConfiguration.class);
		Mockito.when(config.getNodeLimit()).thenReturn(10);
		Mockito.when(config.getTimeSyncNodeLimit()).thenReturn(12);
		return config;
	}

	private static Config createConfig() {
		// Arrange:
		final TrustProvider trustProvider = Mockito.mock(TrustProvider.class);
		Mockito.when(trustProvider.computeTrust(Mockito.any())).thenReturn(new ColumnVector(1, 1, 1));

		final Config config = Mockito.mock(Config.class);
		Mockito.when(config.getTrustProvider()).thenReturn(trustProvider);
		Mockito.when(config.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));
		Mockito.when(config.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));
		Mockito.when(config.getPreTrustedNodes()).thenReturn(new PreTrustedNodes(new HashSet<>()));
		return config;
	}
}