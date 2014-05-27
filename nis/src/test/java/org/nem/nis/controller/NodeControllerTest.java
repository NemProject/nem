package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.metadata.ApplicationMetaData;
import org.nem.deploy.CommonStarter;
import org.nem.nis.NisPeerNetworkHost;
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

		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getProtocol()).thenReturn("https");
		Mockito.when(request.getRemoteAddr()).thenReturn("10.0.0.123");
		Mockito.when(request.getRemotePort()).thenReturn(97);

		// Act:
		final NodeEndpoint endpoint = context.controller.canYouSeeMe(request);

		// Assert:
		Assert.assertThat(endpoint, IsEqual.equalTo(new NodeEndpoint("https", "10.0.0.123", 97)));
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
		private final MockNisPeerNetworkHost host = new MockNisPeerNetworkHost(this.network);
		private final NodeController controller = new NodeController(this.host);
	}

	private static class MockNisPeerNetworkHost extends NisPeerNetworkHost {

		private final PeerNetwork peerNetwork;

		public MockNisPeerNetworkHost(final PeerNetwork peerNetwork) {
			super(null, null);
			this.peerNetwork = peerNetwork;
		}

		@Override
		public PeerNetwork getNetwork() {
			return this.peerNetwork;
		}
	}
}
