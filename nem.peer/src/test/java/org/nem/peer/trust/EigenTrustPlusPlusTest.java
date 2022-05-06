package org.nem.peer.trust;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.node.Node;
import org.nem.peer.test.*;
import org.nem.peer.trust.score.*;

public class EigenTrustPlusPlusTest {

	// region score provider

	@Test
	public void scoreProviderProviderReturnsNumberOfSuccessfulCallsAsTrustScore() {
		// Arrange:
		final ScoreProvider provider = new EigenTrustPlusPlus.ScoreProvider();
		final ScoreProviderTestContext context = new ScoreProviderTestContext(provider);

		// Assert:
		MatcherAssert.assertThat(context.calculateTrustScore(1000, 1), IsEqual.equalTo(1000.0));
		MatcherAssert.assertThat(context.calculateTrustScore(1, 1000), IsEqual.equalTo(1.0));
		MatcherAssert.assertThat(context.calculateTrustScore(1000, 980), IsEqual.equalTo(1000.0));
		MatcherAssert.assertThat(context.calculateTrustScore(21, 1), IsEqual.equalTo(21.0));
	}

	@Test
	public void scoreProviderReturnsDifferentCredibilityScoreForAllInputs() {
		// Arrange:
		final TrustScores scores = new TrustScores();
		final EigenTrustPlusPlus.ScoreProvider provider = new EigenTrustPlusPlus.ScoreProvider();
		provider.setTrustScores(scores);
		final ScoreProviderTestContext context = new ScoreProviderTestContext(provider, scores);

		// Assert:
		MatcherAssert.assertThat(context.calculateCredibilityScore(1, 2, 4, 5), IsEqual.equalTo(-18.0));
		MatcherAssert.assertThat(context.calculateCredibilityScore(4, 5, 2, 1), IsEqual.equalTo(18.0));
		MatcherAssert.assertThat(context.calculateCredibilityScore(1, 1, 1, 1), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(context.calculateCredibilityScore(1, 2, 1, 1), IsEqual.equalTo(1.0));
	}

	// endregion

	// region feedback credibility vector

	@Test
	public void nodeIsCompletelyCredibleToItself() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(3);
		final NodeExperiences experiences = new NodeExperiences();
		final EigenTrustPlusPlus trust = new EigenTrustPlusPlus();

		// Act:
		trust.updateFeedback(nodes[1], nodes, experiences);
		final ColumnVector vector = trust.getCredibilityScores().getScoreVector(nodes[1], nodes);

		// Assert:
		MatcherAssert.assertThat(vector.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(vector.getAt(1), IsEqual.equalTo(1.0));
	}

	@Test
	public void nodesWithoutSharedPartnersHaveNoCredibility() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(3);
		final NodeExperiences experiences = new NodeExperiences();
		final EigenTrustPlusPlus trust = new EigenTrustPlusPlus();

		// Act:
		trust.updateFeedback(nodes[1], nodes, experiences);
		final ColumnVector vector = trust.getCredibilityScores().getScoreVector(nodes[1], nodes);

		// Assert:
		MatcherAssert.assertThat(vector.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(vector.getAt(0), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(vector.getAt(2), IsEqual.equalTo(0.0));
	}

	@Test
	public void nodesWithSharedPartnersHaveCredibilityCorrelatedWithProviderScore() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(4);
		final NodeExperiences experiences = new NodeExperiences();
		final EigenTrustPlusPlus trust = new EigenTrustPlusPlus(new MockScoreProvider(experiences));

		experiences.getNodeExperience(nodes[1], nodes[0]).successfulCalls().set(10);
		experiences.getNodeExperience(nodes[3], nodes[0]).successfulCalls().set(2);
		experiences.getNodeExperience(nodes[1], nodes[2]).successfulCalls().set(11);
		experiences.getNodeExperience(nodes[3], nodes[2]).successfulCalls().set(3);

		// Act:
		trust.updateFeedback(nodes[1], nodes, experiences);
		final ColumnVector vector = trust.getCredibilityScores().getScoreVector(nodes[1], nodes);

		// Assert:
		MatcherAssert.assertThat(vector.size(), IsEqual.equalTo(4));
		Assert.assertEquals(21002.73, vector.getAt(3), 0.1);
	}

	// endregion

	// region trust matrix

	@Test
	public void trustMatrixCanBeCalculatedWithCustomCredibility() {
		// Arrange:
		final TestTrustContext testContext = new TestTrustContext();
		final TrustContext context = testContext.getContext();
		final EigenTrustPlusPlus trust = new EigenTrustPlusPlus();
		final Node[] nodes = context.getNodes();

		trust.getTrustScores().getScore(nodes[0], nodes[1]).score().set(7);
		trust.getCredibilityScores().getScore(nodes[0], nodes[1]).score().set(0.5);
		trust.getTrustScores().getScore(nodes[0], nodes[2]).score().set(3.5);
		trust.getCredibilityScores().getScore(nodes[0], nodes[2]).score().set(0.25);
		trust.getTrustScores().getScore(nodes[1], nodes[0]).score().set(5);
		trust.getCredibilityScores().getScore(nodes[1], nodes[0]).score().set(0.1);

		// Act:
		final Matrix matrix = trust.getTrustMatrix(nodes);

		// Assert:
		MatcherAssert.assertThat(matrix.getRowCount(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(0.8));
		MatcherAssert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(0.2));
		MatcherAssert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(1.0));
	}

	// endregion

	private static class MockScoreProvider implements ScoreProvider {

		private final NodeExperiences nodeExperiences;

		public MockScoreProvider(final NodeExperiences nodeExperiences) {
			this.nodeExperiences = nodeExperiences;
		}

		@Override
		public double calculateTrustScore(final NodeExperience experience) {
			return 0;
		}

		@Override
		public double calculateCredibilityScore(final Node node1, final Node node2, final Node node3) {
			final NodeExperience experience1 = this.nodeExperiences.getNodeExperience(node1, node3);
			final NodeExperience experience2 = this.nodeExperiences.getNodeExperience(node2, node3);
			final long numSuccessfulCalls = experience1.successfulCalls().get() + experience2.successfulCalls().get();
			return 0 == numSuccessfulCalls ? 100 : numSuccessfulCalls;
		}
	}
}
