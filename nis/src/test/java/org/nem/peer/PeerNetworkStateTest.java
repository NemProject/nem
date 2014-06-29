package org.nem.peer;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.peer.node.*;
import org.nem.peer.test.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.util.*;

public class PeerNetworkStateTest {

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
		nodes.update(PeerUtils.createNodeWithName("a1"), NodeStatus.ACTIVE);
		nodes.update(PeerUtils.createNodeWithName("i1"), NodeStatus.INACTIVE);
		nodes.update(PeerUtils.createNodeWithName("f"), NodeStatus.FAILURE);
		nodes.update(PeerUtils.createNodeWithName("i2"), NodeStatus.INACTIVE);
		nodes.update(PeerUtils.createNodeWithName("a2"), NodeStatus.ACTIVE);
		nodes.update(PeerUtils.createNodeWithName("a3"), NodeStatus.ACTIVE);

		// Act:
		final NodeExperiences experiences = new NodeExperiences();
		final PeerNetworkState state = new PeerNetworkState(config, experiences, nodes);
		final TrustContext context = state.getTrustContext();

		// Assert:
		final Node[] expectedNodes = new Node[] {
				PeerUtils.createNodeWithName("a1"),
				PeerUtils.createNodeWithName("a2"),
				PeerUtils.createNodeWithName("a3"),
				PeerUtils.createNodeWithName("i1"),
				PeerUtils.createNodeWithName("i2"),
				PeerUtils.createNodeWithName("l"),
		};
		Assert.assertThat(context.getNodes(), IsEqual.equalTo(expectedNodes));
		Assert.assertThat(context.getLocalNode(), IsSame.sameInstance(state.getLocalNode()));
		Assert.assertThat(context.getNodeExperiences(), IsSame.sameInstance(experiences));
		Assert.assertThat(context.getPreTrustedNodes(), IsSame.sameInstance(preTrustedNodes));
		Assert.assertThat(context.getParams(), IsSame.sameInstance(params));
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
		nodes.update(PeerUtils.createNodeWithName("p"), NodeStatus.INACTIVE);
		final PeerNetworkState state = new PeerNetworkState(config, nodeExperiences, nodes);
		final Node remoteNode = PeerUtils.createNodeWithName(name);

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
		final Node otherNode1 = PeerUtils.createNodeWithPort(91);
		final Node otherNode2 = PeerUtils.createNodeWithPort(97);

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
		pairs.add(new NodeExperiencePair(PeerUtils.createNodeWithPort(81), PeerUtils.createNodeExperience(14)));
		pairs.add(new NodeExperiencePair(PeerUtils.createNodeWithPort(83), PeerUtils.createNodeExperience(44)));

		// Act:
		state.setRemoteNodeExperiences(new NodeExperiencesPair(state.getLocalNode(), pairs));
	}

	@Test
	public void canBatchSetRemoteExperiences() {
		// Arrange:
		final NodeExperiences experiences = new NodeExperiences();
		final PeerNetworkState state = new PeerNetworkState(createTestConfig(), experiences, new NodeCollection());

		final Node remoteNode = PeerUtils.createNodeWithPort(1);
		final Node otherNode1 = PeerUtils.createNodeWithPort(81);
		final Node otherNode2 = PeerUtils.createNodeWithPort(83);

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

	private static Config createTestConfig() {
		final Config config = Mockito.mock(Config.class);
		Mockito.when(config.getLocalNode()).thenReturn(PeerUtils.createNodeWithName("l"));
		return config;
	}

	private static PeerNetworkState createDefaultState() {
		return new PeerNetworkState(
				createTestConfig(),
				new NodeExperiences(),
				new NodeCollection());
	}
}