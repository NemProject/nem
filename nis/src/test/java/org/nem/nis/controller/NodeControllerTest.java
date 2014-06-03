package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.metadata.ApplicationMetaData;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.IsEquivalent;
import org.nem.deploy.CommonStarter;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.viewmodels.ExtendedNodeExperiencePair;
import org.nem.peer.*;
import org.nem.peer.node.*;
import org.nem.peer.test.*;
import org.nem.peer.trust.score.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class NodeControllerTest {

	@Test
	public void getInfoReturnsNetworkLocalNode() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Node node = context.controller.getExtendedInfo().getNode();

		// Assert:
		Assert.assertThat(node, IsSame.sameInstance(context.network.getLocalNode()));
	}

	@Test
	public void getExtendedInfoReturnsNetworkLocalNode() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Node node = context.controller.getExtendedInfo().getNode();

		// Assert:
		Assert.assertThat(node, IsSame.sameInstance(context.network.getLocalNode()));
	}

	@Test
	public void getExtendedInfoReturnsCommonStarterApplicationMetaData() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ApplicationMetaData appMetaData = context.controller.getExtendedInfo().getAppMetaData();

		// Assert:
		Assert.assertThat(appMetaData, IsSame.sameInstance(CommonStarter.META_DATA));
	}

	@Test
	public void getPeerListReturnsNetworkNodes() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final NodeCollection nodes = context.controller.getPeerList();

		// Assert:
		Assert.assertThat(nodes, IsSame.sameInstance(context.network.getNodes()));
	}

	@Test
	public void getExperiencesReturnsExtendedLocalNodeExperiences() {
		// Arrange:
		final TestContext context = new TestContext();
		final PeerNetwork network = context.network;
		network.updateExperience(Node.fromHost("10.0.0.2"), NodeInteractionResult.FAILURE);
		network.updateExperience(Node.fromHost("10.0.0.7"), NodeInteractionResult.SUCCESS);
		network.updateExperience(Node.fromHost("10.0.0.4"), NodeInteractionResult.SUCCESS);

		Mockito.when(context.host.getSyncAttempts(Node.fromHost("10.0.0.2"))).thenReturn(7);
		Mockito.when(context.host.getSyncAttempts(Node.fromHost("10.0.0.7"))).thenReturn(0);
		Mockito.when(context.host.getSyncAttempts(Node.fromHost("10.0.0.4"))).thenReturn(2);

		// Act:
		final Collection<ExtendedNodeExperiencePair> pairs = context.controller.getExperiences().asCollection();

		// Assert:
		final List<ExtendedNodeExperiencePair> expectedPairs = Arrays.asList(
				new ExtendedNodeExperiencePair(Node.fromHost("10.0.0.2"), new NodeExperience(0, 1), 7),
				new ExtendedNodeExperiencePair(Node.fromHost("10.0.0.7"), new NodeExperience(1, 0), 0),
				new ExtendedNodeExperiencePair(Node.fromHost("10.0.0.4"), new NodeExperience(1, 0), 2));
		Assert.assertThat(pairs, IsEquivalent.equivalentTo(expectedPairs));
	}

	@Test
	public void getActivePeerListReturnsActiveNetworkNodes() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeCollection nodeCollection = context.network.getNodes();
		nodeCollection.update(Node.fromHost("10.0.0.2"), NodeStatus.INACTIVE);
		nodeCollection.update(Node.fromHost("10.0.0.4"), NodeStatus.ACTIVE);
		nodeCollection.update(Node.fromHost("10.0.0.3"), NodeStatus.FAILURE);
		nodeCollection.update(Node.fromHost("10.0.0.7"), NodeStatus.ACTIVE);

		// Act:
		final SerializableList<Node> nodes = context.controller.getActivePeerList();

		// Assert:
		final List<Node> expectedNodes = Arrays.asList(
				Node.fromHost("10.0.0.4"),
				Node.fromHost("10.0.0.7"));
		Assert.assertThat(nodes.asCollection(), IsEquivalent.equivalentTo(expectedNodes));
	}

	@Test
	public void pingActivatesSourceNode() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeCollection nodes = context.network.getNodes();

		final Node sourceNode = Utils.createNodeWithPort(111);
		final NodeExperiencesPair pair = new NodeExperiencesPair(sourceNode, new ArrayList<>());

		// Arrange: sanity
		Assert.assertThat(nodes.getNodeStatus(sourceNode), IsEqual.equalTo(NodeStatus.FAILURE));

		// Act:
		context.controller.ping(pair);

		// Assert:
		Assert.assertThat(nodes.getNodeStatus(sourceNode), IsEqual.equalTo(NodeStatus.ACTIVE));
	}

	@Test
	public void pingSetsSourceNodeExperiences() {
		// Arrange:
		final TestContext context = new TestContext();

		final Node sourceNode = Utils.createNodeWithPort(111);
		final Node partnerNode = Utils.createNodeWithPort(222);
		final List<NodeExperiencePair> experiences = new ArrayList<>();
		experiences.add(new NodeExperiencePair(partnerNode, createNodeExperience(12, 34)));
		final NodeExperiencesPair pair = new NodeExperiencesPair(sourceNode, experiences);

		// Arrange: sanity
		Assert.assertThat(
				context.nodeExperiences.getNodeExperience(sourceNode, partnerNode).successfulCalls().get(),
				IsEqual.equalTo(0L));

		// Act:
		context.controller.ping(pair);

		// Assert:
		final NodeExperience experience = context.nodeExperiences.getNodeExperience(sourceNode, partnerNode);
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(12L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(34L));
	}

	@Test
	public void canYouSeeMeReturnsTheRemoteAddress() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeEndpoint localEndpoint = new NodeEndpoint("ftp", "localhost", 123);

		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getScheme()).thenReturn("https");
		Mockito.when(request.getRemoteAddr()).thenReturn("10.0.0.123");
		Mockito.when(request.getRemotePort()).thenReturn(97);

		// Act:
		final NodeEndpoint endpoint = context.controller.canYouSeeMe(localEndpoint, request);

		// Assert:
		// (1) scheme and address come from the servlet request
		// (2) port comes from the original local node endpoint
		Assert.assertThat(endpoint, IsEqual.equalTo(new NodeEndpoint("https", "10.0.0.123", 123)));
	}

	private static NodeExperience createNodeExperience(int numSuccessfulCalls, int numFailureCalls) {
		final NodeExperience experience = new NodeExperience();
		experience.successfulCalls().set(numSuccessfulCalls);
		experience.failedCalls().set(numFailureCalls);
		return experience;
	}

	private static class TestContext {
		private final NodeExperiences nodeExperiences = new NodeExperiences();
		private final MockPeerNetwork network = new MockPeerNetwork(this.nodeExperiences);
		private final NisPeerNetworkHost host;
		private final NodeController controller;

		private TestContext() {
			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.controller = new NodeController(this.host);
		}
	}
}
