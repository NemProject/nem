package org.nem.peer.trust.score;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.peer.test.Utils;
import org.nem.peer.node.Node;

public class NodeExperiencePairTest {

	@Test
	public void pairCanBeCreated() {
		// Arrange:
		final Node node = Utils.createNodeWithPort(81);
		final NodeExperience experience = new NodeExperience();

		// Act:
		final NodeExperiencePair pair = new NodeExperiencePair(node, experience);

		// Assert:
		Assert.assertThat(pair.getNode(), IsSame.sameInstance(node));
		Assert.assertThat(pair.getExperience(), IsSame.sameInstance(experience));
	}

	@Test
	public void pairCanBeRoundTripped() throws Exception {
		// Arrange:
		final Node node = Utils.createNodeWithPort(81);
		final NodeExperience experience = new NodeExperience();
		experience.successfulCalls().set(17);
		final NodeExperiencePair originalPair = new NodeExperiencePair(node, experience);

		// Act:
		final NodeExperiencePair pair = new NodeExperiencePair(org.nem.core.test.Utils.roundtripSerializableEntity(originalPair, null));

		// Assert:
		Assert.assertThat(pair.getNode().getEndpoint().getBaseUrl().getPort(), IsEqual.equalTo(81));
		Assert.assertThat(pair.getExperience().successfulCalls().get(), IsEqual.equalTo(17L));
	}
}