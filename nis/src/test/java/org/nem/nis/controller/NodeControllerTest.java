package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.peer.*;
import org.nem.peer.test.MockPeerNetwork;
import org.nem.peer.test.Utils;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.util.*;

public class NodeControllerTest {

	@Test
	public void getInfoReturnsNetworkLocalNode() {
		// Arrange:
		final MockPeerNetwork network = new MockPeerNetwork();
		final MockNisPeerNetworkHost host = new MockNisPeerNetworkHost(network);
		final NodeController controller = new NodeController(host);

		// Act:
		final Node node = controller.getInfo();

		// Assert:
		Assert.assertThat(node, IsSame.sameInstance(network.getLocalNode()));
	}

	@Test
	public void getPeerListReturnsNetworkNodes() {
		// Arrange:
		final MockPeerNetwork network = new MockPeerNetwork();
		final MockNisPeerNetworkHost host = new MockNisPeerNetworkHost(network);
		final NodeController controller = new NodeController(host);

		// Act:
		final NodeCollection nodes = controller.getPeerList();

		// Assert:
		Assert.assertThat(nodes, IsSame.sameInstance(network.getNodes()));
	}

	@Test
	public void pingActivatesSourceNode() {
		// Arrange:
		final MockPeerNetwork network = new MockPeerNetwork();
		final MockNisPeerNetworkHost host = new MockNisPeerNetworkHost(network);
		final NodeController controller = new NodeController(host);

		final Node sourceNode = Utils.createNodeWithPort(111);
		final NodeExperiencesPair pair = new NodeExperiencesPair(sourceNode, new ArrayList<NodeExperiencePair>());

		// Arrange: sanity
		Assert.assertThat(network.getNodes().getNodeStatus(sourceNode), IsEqual.equalTo(NodeStatus.FAILURE));

		// Act:
		controller.ping(getNodeExperiencesPairDeserializer(pair));

		// Assert:
		Assert.assertThat(network.getNodes().getNodeStatus(sourceNode), IsEqual.equalTo(NodeStatus.ACTIVE));
	}

	@Test
	public void pingSetsSourceNodeExperiences() {
		// Arrange:
		final NodeExperiences nodeExperiences = new NodeExperiences();
		final MockPeerNetwork network = new MockPeerNetwork(nodeExperiences);
		final MockNisPeerNetworkHost host = new MockNisPeerNetworkHost(network);
		final NodeController controller = new NodeController(host);

		final Node sourceNode = Utils.createNodeWithPort(111);
		final Node partnerNode = Utils.createNodeWithPort(222);
		final List<NodeExperiencePair> experiences = new ArrayList<>();
		experiences.add(new NodeExperiencePair(partnerNode, createNodeExperience(12, 34)));
		final NodeExperiencesPair pair = new NodeExperiencesPair(sourceNode, experiences);

		// Arrange: sanity
		Assert.assertThat(
				nodeExperiences.getNodeExperience(sourceNode, partnerNode).successfulCalls().get(),
				IsEqual.equalTo(0L));

		// Act:
		controller.ping(getNodeExperiencesPairDeserializer(pair));

		// Assert:
		final NodeExperience experience = nodeExperiences.getNodeExperience(sourceNode, partnerNode);
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(12L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(34L));
	}

	private static NodeExperience createNodeExperience(int numSuccessfulCalls, int numFailureCalls) {
		final NodeExperience experience = new NodeExperience();
		experience.successfulCalls().set(numSuccessfulCalls);
		experience.failedCalls().set(numFailureCalls);
		return experience;
	}

	private static JsonDeserializer getNodeExperiencesPairDeserializer(final NodeExperiencesPair pair) {
		final JsonSerializer serializer = new JsonSerializer();
		pair.serialize(serializer);
		return new JsonDeserializer(serializer.getObject(), new DeserializationContext(null));
	}

	/**
	 * Mock NisPeerNetworkHost implementation.
	 */
	private static class MockNisPeerNetworkHost extends NisPeerNetworkHost {

		private final PeerNetwork peerNetwork;

		/**
		 * Creates a mock NIS peer network host.
		 *
		 * @param peerNetwork The peer network to host.
		 */
		public MockNisPeerNetworkHost(final PeerNetwork peerNetwork) {
			this.peerNetwork = peerNetwork;
		}

		@Override
		public PeerNetwork getNetwork() {
			return this.peerNetwork;
		}
	}
}
