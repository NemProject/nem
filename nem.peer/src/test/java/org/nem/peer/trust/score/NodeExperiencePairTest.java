package org.nem.peer.trust.score;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.node.*;
import org.nem.core.test.NodeUtils;

public class NodeExperiencePairTest {

	// region basic operations

	@Test
	public void pairCanBeCreated() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("bob");
		final NodeExperience experience = new NodeExperience();

		// Act:
		final NodeExperiencePair pair = new NodeExperiencePair(node, experience);

		// Assert:
		MatcherAssert.assertThat(pair.getNode(), IsSame.sameInstance(node));
		MatcherAssert.assertThat(pair.getExperience(), IsSame.sameInstance(experience));
	}

	@Test
	public void pairCanBeRoundTripped() {
		// Arrange:
		final NodeIdentity identity = new NodeIdentity(new KeyPair());
		final Node node = new Node(identity, NodeEndpoint.fromHost("localhost"));
		final NodeExperience experience = new NodeExperience();
		experience.successfulCalls().set(17);
		final NodeExperiencePair originalPair = new NodeExperiencePair(node, experience);

		// Act:
		final NodeExperiencePair pair = new NodeExperiencePair(org.nem.core.test.Utils.roundtripSerializableEntity(originalPair, null));

		// Assert:
		MatcherAssert.assertThat(pair.getNode(), IsEqual.equalTo(node));
		MatcherAssert.assertThat(pair.getExperience().successfulCalls().get(), IsEqual.equalTo(17L));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeExperiencePair pair = createNodeExperiencePair("10.0.0.1", 5, 1);

		// Assert:
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 5, 1), IsEqual.equalTo(pair));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.2", 5, 1), IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 2, 1), IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 5, 7), IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(5L, IsNot.not(IsEqual.equalTo((Object) pair)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeExperiencePair pair = createNodeExperiencePair("10.0.0.1", 5, 1);
		final int hashCode = pair.hashCode();

		// Assert:
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 5, 1).hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.2", 5, 1).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 2, 1).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 5, 7).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsAppropriateStringRepresentation() {
		// Arrange:
		final NodeExperiencePair pair = createNodeExperiencePair("10.0.0.1", 5, 1);

		// Assert:
		MatcherAssert.assertThat(pair.toString(), IsEqual.equalTo("[success: 5, failure: 1] @ [Node [(Weak Id) 10.0.0.1] @ [10.0.0.1]]"));
	}

	// endregion

	private static NodeExperiencePair createNodeExperiencePair(final String host, final int numSuccess, final int numFailures) {
		return new NodeExperiencePair(NodeUtils.createNodeWithHost(host), new NodeExperience(numSuccess, numFailures));
	}
}
