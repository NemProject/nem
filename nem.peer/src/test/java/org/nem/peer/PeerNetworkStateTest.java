package org.nem.peer;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsSame;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.model.primitive.NodeAge;
import org.nem.core.model.primitive.TimeOffset;
import org.nem.core.node.NisPeerId;
import org.nem.core.node.Node;
import org.nem.core.node.NodeCollection;
import org.nem.core.node.NodeStatus;
import org.nem.core.test.NodeUtils;
import org.nem.core.time.TimeInstant;
import org.nem.core.time.TimeSynchronizationResult;
import org.nem.peer.test.NodeCollectionAssert;
import org.nem.peer.test.PeerUtils;
import org.nem.peer.trust.PreTrustedNodes;
import org.nem.peer.trust.TrustContext;
import org.nem.peer.trust.TrustParameters;
import org.nem.peer.trust.score.NodeExperience;
import org.nem.peer.trust.score.NodeExperiencePair;
import org.nem.peer.trust.score.NodeExperiences;
import org.nem.peer.trust.score.NodeExperiencesPair;

import java.util.*;

public class PeerNetworkStateTest {

	//region constructor

	@Test
	public void ctorAddsAllWellKnownPeersAsActive() {
		// Act:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(state.getNodes(), new String[] { "a", "b", "c" }, new String[] {});
	}

	//endregion

	//region getters

	@Test
	public void getConfigurationReturnsConstructorParameter() {
		// Act:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Assert:
		Assert.assertThat(state.getConfiguration(), IsSame.sameInstance(config));
	}

	@Test
	public void getNodesReturnsConstructorParameter() {
		// Act:
		final NodeCollection nodes = new NodeCollection();
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), new NodeExperiences(), nodes);

		// Assert:
		Assert.assertThat(state.getNodes(), IsSame.sameInstance(nodes));
	}

	@Test
	public void getLocalNodeReturnsConfigLocalNode() {
		// Act:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Assert:
		Assert.assertThat(state.getLocalNode(), IsEqual.equalTo(config.getLocalNode()));
	}

	@Test
	public void getTrustContextReturnsAppropriateTrustContext() {
		// Arrange:
		final PreTrustedNodes preTrustedNodes = new PreTrustedNodes(new HashSet<>());
		final TrustParameters params = new TrustParameters();
		final Config config = createTestConfig();
		Mockito.when(config.getPreTrustedNodes()).thenReturn(preTrustedNodes);
		Mockito.when(config.getTrustParameters()).thenReturn(params);

		final NodeCollection nodes = new NodeCollection();
		nodes.update(NodeUtils.createNodeWithName("a1"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("b1"), NodeStatus.BUSY);
		nodes.update(NodeUtils.createNodeWithName("f"), NodeStatus.FAILURE);
		nodes.update(NodeUtils.createNodeWithName("b2"), NodeStatus.BUSY);
		nodes.update(NodeUtils.createNodeWithName("a2"), NodeStatus.ACTIVE);
		nodes.update(NodeUtils.createNodeWithName("a3"), NodeStatus.ACTIVE);

		// Act:
		final NodeExperiences experiences = new NodeExperiences();
		final PeerNetworkState state = new PeerNetworkState(config, experiences, nodes);
		final TrustContext context = state.getTrustContext();

		// Assert:
		final Node[] expectedNodes = new Node[] {
				NodeUtils.createNodeWithName("a1"),
				NodeUtils.createNodeWithName("a2"),
				NodeUtils.createNodeWithName("a3"),
				NodeUtils.createNodeWithName("b2"),
				NodeUtils.createNodeWithName("b1"),
				NodeUtils.createNodeWithName("l"),
		};
		Assert.assertThat(context.getNodes(), IsEqual.equalTo(expectedNodes));
		Assert.assertThat(context.getLocalNode(), IsSame.sameInstance(state.getLocalNode()));
		Assert.assertThat(context.getNodeExperiences(), IsSame.sameInstance(experiences));
		Assert.assertThat(context.getPreTrustedNodes(), IsSame.sameInstance(preTrustedNodes));
		Assert.assertThat(context.getParams(), IsSame.sameInstance(params));
	}

	@Test
	public void isChainSynchronizedReturnsFalseAsDefault() {
		// Act:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Assert:
		Assert.assertThat(state.isChainSynchronized(), IsEqual.equalTo(false));
	}

	@Test
	public void isChainSynchronizedReturnsTrueWhenSetToTrueAndThenToFalse() {
		// Arrange:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Act:
		state.setChainSynchronized(true);
		state.setChainSynchronized(false);

		// Assert:
		Assert.assertThat(state.isChainSynchronized(), IsEqual.equalTo(true));
	}

	@Test
	public void isChainSynchronizedReturnsFalseWhenSetToTrueAndThenToFalseTwice() {
		// Arrange:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Act:
		state.setChainSynchronized(true);
		state.setChainSynchronized(false);
		state.setChainSynchronized(false);

		// Assert:
		Assert.assertThat(state.isChainSynchronized(), IsEqual.equalTo(false));
	}

	//endregion

	//region updateExperience

	@Test
	public void updateExperienceUpdatesPartnerExperienceOnSuccess() {
		// Act:
		final NodeExperience experience = updateExperienceThrice("p", NodeInteractionResult.SUCCESS);

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(3L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(3L));
	}

	@Test
	public void updateExperienceUpdatesPartnerExperienceOnFailure() {
		// Act:
		final NodeExperience experience = updateExperienceThrice("p", NodeInteractionResult.FAILURE);

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(3L));
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(3L));
	}

	@Test
	public void updateExperienceDoesNotUpdatePartnerExperienceOnNeutral() {
		// Act:
		final NodeExperience experience = updateExperienceThrice("p", NodeInteractionResult.NEUTRAL);

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(0L));
	}

	@Test
	public void updateExperienceUpdatesUnknownNodeExperience() {
		// Act:
		final NodeExperience experience = updateExperienceThrice("z", NodeInteractionResult.SUCCESS);

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(3L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(3L));
	}

	@Test
	public void updateExperienceDoesNotUpdateLocalNodeExperience() {
		// Act:
		final NodeExperience experience = updateExperienceThrice("l", NodeInteractionResult.SUCCESS);

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(0L));
	}

	private static NodeExperience updateExperienceThrice(final String name, final NodeInteractionResult result) {
		// Arrange:
		final Config config = createTestConfig();
		final NodeExperiences nodeExperiences = new NodeExperiences();
		final NodeCollection nodes = new NodeCollection();
		nodes.update(NodeUtils.createNodeWithName("p"), NodeStatus.INACTIVE);
		final PeerNetworkState state = new PeerNetworkState(config, nodeExperiences, nodes);
		final Node remoteNode = NodeUtils.createNodeWithName(name);

		// Act:
		state.updateExperience(remoteNode, result);
		state.updateExperience(remoteNode, result);
		state.updateExperience(remoteNode, result);
		return nodeExperiences.getNodeExperience(state.getLocalNode(), remoteNode);
	}

	//endregion

	//region getLocalNodeAndExperiences / setRemoteNodeExperiences

	@Test
	public void getLocalNodeAndExperiencesIncludesLocalNode() {
		// Arrange:
		final PeerNetworkState state = createDefaultState();

		// Act:
		final NodeExperiencesPair pair = state.getLocalNodeAndExperiences();

		// Assert:
		Assert.assertThat(pair.getNode(), IsSame.sameInstance(state.getLocalNode()));
	}

	@Test
	public void getLocalNodeAndExperiencesIncludesLocalNodeExperiences() {
		// Arrange:
		final NodeExperiences experiences = new NodeExperiences();
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), experiences, new NodeCollection());

		final Node localNode = state.getLocalNode();
		final Node otherNode1 = NodeUtils.createNodeWithPort(91);
		final Node otherNode2 = NodeUtils.createNodeWithPort(97);

		experiences.getNodeExperience(localNode, otherNode1).successfulCalls().set(14);
		experiences.getNodeExperience(localNode, otherNode2).successfulCalls().set(7);

		// Act:
		final List<NodeExperiencePair> localNodeExperiences = state.getLocalNodeAndExperiences().getExperiences();

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
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), experiences, new NodeCollection());

		final List<NodeExperiencePair> pairs = new ArrayList<>();
		pairs.add(new NodeExperiencePair(NodeUtils.createNodeWithPort(81), PeerUtils.createNodeExperience(14)));
		pairs.add(new NodeExperiencePair(NodeUtils.createNodeWithPort(83), PeerUtils.createNodeExperience(44)));

		// Act:
		state.setRemoteNodeExperiences(new NodeExperiencesPair(state.getLocalNode(), pairs));
	}

	@Test
	public void canBatchSetRemoteExperiences() {
		// Arrange:
		final NodeExperiences experiences = new NodeExperiences();
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), experiences, new NodeCollection());

		final Node remoteNode = NodeUtils.createNodeWithPort(1);
		final Node otherNode1 = NodeUtils.createNodeWithPort(81);
		final Node otherNode2 = NodeUtils.createNodeWithPort(83);

		final List<NodeExperiencePair> pairs = Arrays.asList(
				new NodeExperiencePair(otherNode1, PeerUtils.createNodeExperience(14)),
				new NodeExperiencePair(otherNode2, PeerUtils.createNodeExperience(44)));

		// Act:
		state.setRemoteNodeExperiences(new NodeExperiencesPair(remoteNode, pairs));
		final NodeExperience experience1 = experiences.getNodeExperience(remoteNode, otherNode1);
		final NodeExperience experience2 = experiences.getNodeExperience(remoteNode, otherNode2);

		// Assert:
		Assert.assertThat(experience1.successfulCalls().get(), IsEqual.equalTo(14L));
		Assert.assertThat(experience2.successfulCalls().get(), IsEqual.equalTo(44L));
	}

	//endregion

	//region node age

	@Test
	public void nodeAgeInitiallyIsZero() {
		// Arrange:
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), new NodeExperiences(), new NodeCollection());

		// Assert:
		Assert.assertThat(state.getNodeAge(), IsEqual.equalTo(new NodeAge(0)));
	}

	//endregion

	//region updateTimeSynchronizationResults

	@Test
	public void updateTimeSynchronizationResultsAddsOneToNodeAge() {
		// Arrange:
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), new NodeExperiences(), new NodeCollection());

		// Act:
		state.updateTimeSynchronizationResults(new TimeSynchronizationResult(new TimeInstant(5), new TimeOffset(10), new TimeOffset(20)));
		state.updateTimeSynchronizationResults(new TimeSynchronizationResult(new TimeInstant(15), new TimeOffset(20), new TimeOffset(30)));
		state.updateTimeSynchronizationResults(new TimeSynchronizationResult(new TimeInstant(25), new TimeOffset(30), new TimeOffset(40)));

		// Assert:
		Assert.assertThat(state.getNodeAge(), IsEqual.equalTo(new NodeAge(3)));
	}

	@Test
	public void updateTimeSynchronizationResultsAddsTimeSynchronizationResultToList() {
		// Arrange:
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), new NodeExperiences(), new NodeCollection());

		// Act:
		state.updateTimeSynchronizationResults(new TimeSynchronizationResult(new TimeInstant(5), new TimeOffset(10), new TimeOffset(20)));
		state.updateTimeSynchronizationResults(new TimeSynchronizationResult(new TimeInstant(15), new TimeOffset(20), new TimeOffset(30)));
		state.updateTimeSynchronizationResults(new TimeSynchronizationResult(new TimeInstant(25), new TimeOffset(30), new TimeOffset(40)));

		// Assert:
		Assert.assertThat(state.getTimeSynchronizationResults().size(), IsEqual.equalTo(3));
	}

	//endregion

	//region broadcast buffer

	@Test
	public void broadcastBufferIsInitiallyEmpty() {
		// Arrange:
		final PeerNetworkState state = createDefaultState();

		// Assert:
		Assert.assertThat(state.numBroadcastableEntities(), IsEqual.equalTo(0));
	}

	@Test
	public void addToBroadcastBufferAddsEntityToBuffer() {
		// Arrange:
		final PeerNetworkState state = createDefaultState();

		// Act:
		state.addToBroadcastBuffer(NisPeerId.REST_BLOCK_AT, new BlockHeight(123));

		// Assert:
		Assert.assertThat(state.numBroadcastableEntities(), IsEqual.equalTo(1));
	}

	@Test
	public void getBroadcastableEntitiesReturnsExpectedCollection() {
		// Arrange:
		final PeerNetworkState state = createDefaultState();
		state.addToBroadcastBuffer(NisPeerId.REST_BLOCK_AT, new BlockHeight(123));

		// Act:
		Collection<BroadcastableEntityList> pairs = state.getBroadcastableEntities();

		// Assert:
		Assert.assertThat(pairs.size(), IsEqual.equalTo(1));
		final BroadcastableEntityList pair = pairs.stream().findFirst().get();
		Assert.assertThat(pair.getApiId(), IsEqual.equalTo(NisPeerId.REST_BLOCK_AT));
		Assert.assertThat(pair.getEntities().asCollection(), IsEqual.equalTo(Collections.singletonList(new BlockHeight(123))));
	}

	//endregion

	private static Config createTestConfig() {
		final Config config = Mockito.mock(Config.class);
		Mockito.when(config.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));
		Mockito.when(config.getPreTrustedNodes()).
				thenReturn(new PreTrustedNodes(new HashSet<>(PeerUtils.createNodesWithNames("a", "b", "c"))));
		return config;
	}

	private static PeerNetworkState createDefaultState() {
		return new PeerNetworkState(
				createTestConfig(),
				new NodeExperiences(),
				new NodeCollection());
	}
}