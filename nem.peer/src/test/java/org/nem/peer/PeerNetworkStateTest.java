package org.nem.peer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.*;
import org.nem.core.node.*;
import org.nem.core.test.NodeUtils;
import org.nem.core.time.*;
import org.nem.peer.test.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.util.*;

public class PeerNetworkStateTest {

	// region constructor

	@Test
	public void ctorAddsAllWellKnownPeersAsActive() {
		// Act:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Assert:
		NodeCollectionAssert.areNamesEquivalent(state.getNodes(), new String[]{
				"a", "b", "c"
		}, new String[]{});
	}

	// endregion

	// region getters

	@Test
	public void getConfigurationReturnsConstructorParameter() {
		// Act:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Assert:
		MatcherAssert.assertThat(state.getConfiguration(), IsSame.sameInstance(config));
	}

	@Test
	public void getNodesReturnsConstructorParameter() {
		// Act:
		final NodeCollection nodes = new NodeCollection();
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), new NodeExperiences(), nodes);

		// Assert:
		MatcherAssert.assertThat(state.getNodes(), IsSame.sameInstance(nodes));
	}

	@Test
	public void getLocalNodeReturnsConfigLocalNode() {
		// Act:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Assert:
		MatcherAssert.assertThat(state.getLocalNode(), IsEqual.equalTo(config.getLocalNode()));
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
		final Node[] expectedNodes = new Node[]{
				NodeUtils.createNodeWithName("a1"), NodeUtils.createNodeWithName("a2"), NodeUtils.createNodeWithName("a3"),
				NodeUtils.createNodeWithName("b2"), NodeUtils.createNodeWithName("b1"), NodeUtils.createNodeWithName("l"),
		};
		MatcherAssert.assertThat(context.getNodes(), IsEqual.equalTo(expectedNodes));
		MatcherAssert.assertThat(context.getLocalNode(), IsSame.sameInstance(state.getLocalNode()));
		MatcherAssert.assertThat(context.getNodeExperiences(), IsSame.sameInstance(experiences));
		MatcherAssert.assertThat(context.getPreTrustedNodes(), IsSame.sameInstance(preTrustedNodes));
		MatcherAssert.assertThat(context.getParams(), IsSame.sameInstance(params));
	}

	@Test
	public void isChainSynchronizedReturnsFalseAsDefault() {
		// Act:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Assert:
		MatcherAssert.assertThat(state.isChainSynchronized(), IsEqual.equalTo(false));
	}

	@Test
	public void isChainSynchronizedReturnsTrueWhenSetToTrueAndThenToFalseThreeTimes() {
		// Arrange:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Act:
		state.setChainSynchronized(true);
		state.setChainSynchronized(false);
		state.setChainSynchronized(false);
		state.setChainSynchronized(false);

		// Assert:
		MatcherAssert.assertThat(state.isChainSynchronized(), IsEqual.equalTo(true));
	}

	@Test
	public void isChainSynchronizedReturnsFalseWhenSetToTrueAndThenToFalseFourTimes() {
		// Arrange:
		final Config config = createTestConfig();
		final PeerNetworkState state = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());

		// Act:
		state.setChainSynchronized(true);
		state.setChainSynchronized(false);
		state.setChainSynchronized(false);
		state.setChainSynchronized(false);
		state.setChainSynchronized(false);

		// Assert:
		MatcherAssert.assertThat(state.isChainSynchronized(), IsEqual.equalTo(false));
	}

	// endregion

	// region updateExperience

	@Test
	public void updateExperienceUpdatesPartnerExperienceOnSuccess() {
		// Act:
		final NodeExperience experience = updateExperienceThrice("p", NodeInteractionResult.SUCCESS);

		// Assert:
		MatcherAssert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(3L));
		MatcherAssert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(experience.totalCalls(), IsEqual.equalTo(3L));
	}

	@Test
	public void updateExperienceUpdatesPartnerExperienceOnFailure() {
		// Act:
		final NodeExperience experience = updateExperienceThrice("p", NodeInteractionResult.FAILURE);

		// Assert:
		MatcherAssert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(3L));
		MatcherAssert.assertThat(experience.totalCalls(), IsEqual.equalTo(3L));
	}

	@Test
	public void updateExperienceDoesNotUpdatePartnerExperienceOnNeutral() {
		// Act:
		final NodeExperience experience = updateExperienceThrice("p", NodeInteractionResult.NEUTRAL);

		// Assert:
		MatcherAssert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(experience.totalCalls(), IsEqual.equalTo(0L));
	}

	@Test
	public void updateExperienceUpdatesUnknownNodeExperience() {
		// Act:
		final NodeExperience experience = updateExperienceThrice("z", NodeInteractionResult.SUCCESS);

		// Assert:
		MatcherAssert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(3L));
		MatcherAssert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(experience.totalCalls(), IsEqual.equalTo(3L));
	}

	@Test
	public void updateExperienceDoesNotUpdateLocalNodeExperience() {
		// Act:
		final NodeExperience experience = updateExperienceThrice("l", NodeInteractionResult.SUCCESS);

		// Assert:
		MatcherAssert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(experience.totalCalls(), IsEqual.equalTo(0L));
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

	// endregion

	// region getLocalNodeAndExperiences / setRemoteNodeExperiences

	@Test
	public void getLocalNodeAndExperiencesIncludesLocalNode() {
		// Arrange:
		final PeerNetworkState state = createDefaultState();

		// Act:
		final NodeExperiencesPair pair = state.getLocalNodeAndExperiences();

		// Assert:
		MatcherAssert.assertThat(pair.getNode(), IsSame.sameInstance(state.getLocalNode()));
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
		MatcherAssert.assertThat(pair1.getNode(), IsEqual.equalTo(otherNode1));
		MatcherAssert.assertThat(pair1.getExperience().successfulCalls().get(), IsEqual.equalTo(14L));
		MatcherAssert.assertThat(pair2.getNode(), IsEqual.equalTo(otherNode2));
		MatcherAssert.assertThat(pair2.getExperience().successfulCalls().get(), IsEqual.equalTo(7L));
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
		state.setRemoteNodeExperiences(new NodeExperiencesPair(state.getLocalNode(), pairs), new TimeInstant(123));
	}

	@Test
	public void canBatchSetRemoteExperiences() {
		// Arrange:
		final NodeExperiences experiences = new NodeExperiences();
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), experiences, new NodeCollection());

		final Node remoteNode = NodeUtils.createNodeWithPort(1);
		final Node otherNode1 = NodeUtils.createNodeWithPort(81);
		final Node otherNode2 = NodeUtils.createNodeWithPort(83);

		final List<NodeExperiencePair> pairs = Arrays.asList(new NodeExperiencePair(otherNode1, PeerUtils.createNodeExperience(14)),
				new NodeExperiencePair(otherNode2, PeerUtils.createNodeExperience(44)));

		// Act:
		state.setRemoteNodeExperiences(new NodeExperiencesPair(remoteNode, pairs), new TimeInstant(123));
		final NodeExperience experience1 = experiences.getNodeExperience(remoteNode, otherNode1);
		final NodeExperience experience2 = experiences.getNodeExperience(remoteNode, otherNode2);

		// Assert:
		MatcherAssert.assertThat(experience1.successfulCalls().get(), IsEqual.equalTo(14L));
		MatcherAssert.assertThat(experience2.successfulCalls().get(), IsEqual.equalTo(44L));
		MatcherAssert.assertThat(experience1.getLastUpdateTime(), IsEqual.equalTo(new TimeInstant(123)));
		MatcherAssert.assertThat(experience2.getLastUpdateTime(), IsEqual.equalTo(new TimeInstant(123)));
	}

	// endregion

	// region prune node experiences

	@Test
	public void prunePrunesNodeExperiences() {
		// Arrange:
		final NodeExperiences nodeExperiences = Mockito.mock(NodeExperiences.class);
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), nodeExperiences, new NodeCollection());

		// Act:
		state.pruneNodeExperiences(new TimeInstant(123));

		// Assert:
		Mockito.verify(nodeExperiences, Mockito.only()).prune(new TimeInstant(123));
	}

	// endregion

	// region node age

	@Test
	public void nodeAgeInitiallyIsZero() {
		// Arrange:
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), new NodeExperiences(), new NodeCollection());

		// Assert:
		MatcherAssert.assertThat(state.getNodeAge(), IsEqual.equalTo(new NodeAge(0)));
	}

	// endregion

	// region updateTimeSynchronizationResults

	@Test
	public void updateTimeSynchronizationResultsAddsOneToNodeAge() {
		// Arrange:
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), new NodeExperiences(), new NodeCollection());

		// Act:
		state.updateTimeSynchronizationResults(new TimeSynchronizationResult(new TimeInstant(5), new TimeOffset(10), new TimeOffset(20)));
		state.updateTimeSynchronizationResults(new TimeSynchronizationResult(new TimeInstant(15), new TimeOffset(20), new TimeOffset(30)));
		state.updateTimeSynchronizationResults(new TimeSynchronizationResult(new TimeInstant(25), new TimeOffset(30), new TimeOffset(40)));

		// Assert:
		MatcherAssert.assertThat(state.getNodeAge(), IsEqual.equalTo(new NodeAge(3)));
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
		MatcherAssert.assertThat(state.getTimeSynchronizationResults().size(), IsEqual.equalTo(3));
	}

	// endregion

	private static Config createTestConfig() {
		final Config config = Mockito.mock(Config.class);
		Mockito.when(config.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));
		Mockito.when(config.getPreTrustedNodes())
				.thenReturn(new PreTrustedNodes(new HashSet<>(PeerUtils.createNodesWithNames("a", "b", "c"))));
		return config;
	}

	private static PeerNetworkState createDefaultState() {
		return new PeerNetworkState(createTestConfig(), new NodeExperiences(), new NodeCollection());
	}
}
