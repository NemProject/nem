package org.nem.peer.trust.score;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.peer.test.Utils;
import org.nem.peer.node.Node;

import java.util.*;

public class NodeExperiencesPairTest {

	@Test
	public void pairCanBeCreated() {
		// Arrange:
		final Node node = Utils.createNodeWithPort(81);
		final List<NodeExperiencePair> experiences = new ArrayList<>();

		// Act:
		final NodeExperiencesPair pair = new NodeExperiencesPair(node, experiences);

		// Assert:
		Assert.assertThat(pair.getNode(), IsSame.sameInstance(node));
		Assert.assertThat(pair.getExperiences(), IsSame.sameInstance(experiences));
	}

	@Test
	public void pairCanBeRoundTripped() throws Exception {
		// Arrange:
		final Node node = Utils.createNodeWithPort(81);
		final List<NodeExperiencePair> experiences = new ArrayList<>();
		experiences.add(new NodeExperiencePair(Utils.createNodeWithPort(87), new NodeExperience()));
		experiences.add(new NodeExperiencePair(Utils.createNodeWithPort(90), new NodeExperience()));
		final NodeExperiencesPair originalPair = new NodeExperiencesPair(node, experiences);

		// Act:
		final NodeExperiencesPair pair = new NodeExperiencesPair(org.nem.core.test.Utils.roundtripSerializableEntity(originalPair, null));

		// Assert:
		Assert.assertThat(pair.getNode().getEndpoint().getBaseUrl().getPort(), IsEqual.equalTo(81));
		Assert.assertThat(pair.getExperiences().size(), IsEqual.equalTo(2));
	}
}
