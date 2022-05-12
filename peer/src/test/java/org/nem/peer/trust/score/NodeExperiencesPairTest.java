package org.nem.peer.trust.score;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.node.*;
import org.nem.core.test.NodeUtils;

import java.util.*;

public class NodeExperiencesPairTest {

	@Test
	public void pairCanBeCreated() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("bob");
		final List<NodeExperiencePair> experiences = new ArrayList<>();

		// Act:
		final NodeExperiencesPair pair = new NodeExperiencesPair(node, experiences);

		// Assert:
		MatcherAssert.assertThat(pair.getNode(), IsSame.sameInstance(node));
		MatcherAssert.assertThat(pair.getExperiences(), IsSame.sameInstance(experiences));
	}

	@Test
	public void pairCanBeRoundTripped() throws Exception {
		// Arrange:
		final NodeIdentity identity = new NodeIdentity(new KeyPair());
		final Node node = new Node(identity, NodeEndpoint.fromHost("localhost"));
		final List<NodeExperiencePair> experiences = new ArrayList<>();
		experiences.add(new NodeExperiencePair(NodeUtils.createNodeWithName("a"), new NodeExperience()));
		experiences.add(new NodeExperiencePair(NodeUtils.createNodeWithName("b"), new NodeExperience()));
		final NodeExperiencesPair originalPair = new NodeExperiencesPair(node, experiences);

		// Act:
		final NodeExperiencesPair pair = new NodeExperiencesPair(org.nem.core.test.Utils.roundtripSerializableEntity(originalPair, null));

		// Assert:
		MatcherAssert.assertThat(pair.getNode(), IsEqual.equalTo(node));
		MatcherAssert.assertThat(pair.getExperiences().size(), IsEqual.equalTo(2));
	}
}
