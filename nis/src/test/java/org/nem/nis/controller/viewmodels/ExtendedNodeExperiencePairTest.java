package org.nem.nis.controller.viewmodels;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;
import org.nem.peer.trust.score.NodeExperience;

public class ExtendedNodeExperiencePairTest {

	// region basic operations

	@Test
	public void pairCanBeCreated() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithPort(81);
		final NodeExperience experience = new NodeExperience();

		// Act:
		final ExtendedNodeExperiencePair pair = new ExtendedNodeExperiencePair(node, experience, 89);

		// Assert:
		MatcherAssert.assertThat(pair.getNode(), IsSame.sameInstance(node));
		MatcherAssert.assertThat(pair.getExperience(), IsSame.sameInstance(experience));
		MatcherAssert.assertThat(pair.getNumSyncAttempts(), IsEqual.equalTo(89));
	}

	@Test
	public void pairCanBeRoundTripped() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithPort(81);
		final NodeExperience experience = new NodeExperience();
		experience.successfulCalls().set(17);
		final ExtendedNodeExperiencePair originalPair = new ExtendedNodeExperiencePair(node, experience, 89);

		// Act:
		final ExtendedNodeExperiencePair pair = new ExtendedNodeExperiencePair(
				org.nem.core.test.Utils.roundtripSerializableEntity(originalPair, null));

		// Assert:
		MatcherAssert.assertThat(pair.getNode().getEndpoint().getBaseUrl().getPort(), IsEqual.equalTo(81));
		MatcherAssert.assertThat(pair.getExperience().successfulCalls().get(), IsEqual.equalTo(17L));
		MatcherAssert.assertThat(pair.getNumSyncAttempts(), IsEqual.equalTo(89));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final ExtendedNodeExperiencePair pair = createNodeExperiencePair("10.0.0.1", 5, 1, 9);

		// Assert:
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 5, 1, 9), IsEqual.equalTo(pair));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.2", 5, 1, 9), IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 2, 1, 9), IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 5, 7, 9), IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 5, 1, 8), IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(5L, IsNot.not(IsEqual.equalTo((Object) pair)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final ExtendedNodeExperiencePair pair = createNodeExperiencePair("10.0.0.1", 5, 1, 9);
		final int hashCode = pair.hashCode();

		// Assert:
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 5, 1, 9).hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.2", 5, 1, 9).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 2, 1, 9).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 5, 7, 9).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(createNodeExperiencePair("10.0.0.1", 5, 1, 8).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsAppropriateStringRepresentation() {
		// Arrange:
		final ExtendedNodeExperiencePair pair = createNodeExperiencePair("10.0.0.1", "bob", 5, 1, 9);

		// Assert:
		MatcherAssert.assertThat(pair.toString(), IsEqual.equalTo("[success: 5, failure: 1] @ [Node [(Weak Id) bob] @ [10.0.0.1]]"));
	}

	// endregion

	private static ExtendedNodeExperiencePair createNodeExperiencePair(final String host, final int numSuccess, final int numFailures,
			final int numSyncAttempts) {
		return createNodeExperiencePair(host, host, numSuccess, numFailures, numSyncAttempts);
	}

	private static ExtendedNodeExperiencePair createNodeExperiencePair(final String host, final String name, final int numSuccess,
			final int numFailures, final int numSyncAttempts) {
		return new ExtendedNodeExperiencePair(NodeUtils.createNodeWithHost(host, name), new NodeExperience(numSuccess, numFailures),
				numSyncAttempts);
	}
}
