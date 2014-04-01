package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.peer.Node;
import org.nem.peer.test.ScoreProviderTestContext;
import org.nem.peer.test.TestTrustContext;
import org.nem.peer.trust.score.*;

public class EigenTrustTest {

	//region score provider

	@Test
	public void scoreProviderReturnsDifferenceOfSuccessfulAndFailureCallsAsTrustScore() {
		// Arrange:
		final ScoreProvider provider = new EigenTrust.ScoreProvider();
		final ScoreProviderTestContext context = new ScoreProviderTestContext(provider);

		// Assert:
		Assert.assertThat(context.calculateTrustScore(1000, 1), IsEqual.equalTo(999.0));
		Assert.assertThat(context.calculateTrustScore(1, 1000), IsEqual.equalTo(0.0));
		Assert.assertThat(context.calculateTrustScore(1000, 980), IsEqual.equalTo(20.0));
		Assert.assertThat(context.calculateTrustScore(21, 1), IsEqual.equalTo(20.0));
	}

	@Test
	public void scoreProviderReturnsSameCredibilityScoreForAllInputs() {
		// Arrange:
		final TrustScores scores = new TrustScores();
		final ScoreProvider provider = new EigenTrust.ScoreProvider();
		final ScoreProviderTestContext context = new ScoreProviderTestContext(provider, scores);

		// Assert:
		Assert.assertThat(context.calculateCredibilityScore(1, 2, 4, 5), IsEqual.equalTo(0.0));
		Assert.assertThat(context.calculateCredibilityScore(4, 5, 2, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(context.calculateCredibilityScore(1, 1, 1, 1), IsEqual.equalTo(0.0));
		Assert.assertThat(context.calculateCredibilityScore(1, 2, 1, 1), IsEqual.equalTo(0.0));
	}

	//endregion

	//region updateLocalTrust

	@Test
	public void localTrustIsSetToCorrectDefaultsWhenNoCallsAreMadeBetweenNodes() {
		// Arrange:
		final TestTrustContext testContext = new TestTrustContext();
		final TrustContext context = testContext.getContext();
		final EigenTrust trust = new EigenTrust();
		final TrustScores trustScores = trust.getTrustScores();
		final Node localNode = context.getLocalNode();

		// Act:
		trust.updateTrust(localNode, context);
		final Vector vector = trustScores.getScoreVector(localNode, context.getNodes());
		final RealDouble sum = trustScores.getScoreWeight(localNode);

		// Assert:
		Assert.assertThat(sum.get(), IsEqual.equalTo(3.0));
		Assert.assertThat(vector.getSize(), IsEqual.equalTo(5));
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(1.0 / 3)); // pre-trusted
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(0.0));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(0.0));
		Assert.assertThat(vector.getAt(3), IsEqual.equalTo(1.0 / 3)); // pre-trusted
		Assert.assertThat(vector.getAt(4), IsEqual.equalTo(1.0 / 3)); // self
	}

	@Test
	public void localTrustIsSetToCorrectValuesWhenCallsAreMadeBetweenNodes() {
		// Arrange:
		final TestTrustContext testContext = new TestTrustContext();
		final TrustContext context = testContext.getContext();
		final EigenTrust trust = new EigenTrust(new MockScoreProvider());
		final TrustScores trustScores = trust.getTrustScores();
		final Node localNode = context.getLocalNode();

		testContext.setCallCounts(0, 1, 2); // 2
		testContext.setCallCounts(1, 2, 2); // 4
		testContext.setCallCounts(2, 3, 3); // 9
		testContext.setCallCounts(3, 4, 4); // 16

		// Act:
		trust.updateTrust(localNode, context);
		final Vector vector = trustScores.getScoreVector(localNode, context.getNodes());
		final RealDouble sum = trustScores.getScoreWeight(localNode);

		// Assert:
		Assert.assertThat(sum.get(), IsEqual.equalTo(32.0));
		Assert.assertThat(vector.getSize(), IsEqual.equalTo(5));
		Assert.assertThat(vector.getAt(0), IsEqual.equalTo(2.0 / 32)); // pre-trusted
		Assert.assertThat(vector.getAt(1), IsEqual.equalTo(4.0 / 32));
		Assert.assertThat(vector.getAt(2), IsEqual.equalTo(9.0 / 32));
		Assert.assertThat(vector.getAt(3), IsEqual.equalTo(16.0 / 32)); // pre-trusted
		Assert.assertThat(vector.getAt(4), IsEqual.equalTo(1.0 / 32)); // self
	}

	//endregion

	//region getTrustMatrix

	@Test
	public void localTrustMatrixIsSetToCorrectValues() {
		// Arrange:
		final TestTrustContext testContext = new TestTrustContext();
		final TrustContext context = testContext.getContext();
		final EigenTrust trust = new EigenTrust(new MockScoreProvider());
		final Node localNode = context.getLocalNode();

		testContext.setCallCounts(0, 1, 2); // 2
		testContext.setCallCounts(1, 2, 2); // 4
		testContext.setCallCounts(2, 3, 3); // 9
		testContext.setCallCounts(3, 4, 4); // 16

		// Act:
		trust.updateTrust(localNode, context);
		final Matrix matrix = trust.getTrustMatrix(context.getNodes());

		// Assert:
		Assert.assertThat(matrix.sum(), IsEqual.equalTo(1.0));
		Assert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(5));
		Assert.assertThat(matrix.getRowCount(), IsEqual.equalTo(5));
		Assert.assertThat(matrix.getAt(0, 4), IsEqual.equalTo(2.0 / 32)); // pre-trusted
		Assert.assertThat(matrix.getAt(1, 4), IsEqual.equalTo(4.0 / 32));
		Assert.assertThat(matrix.getAt(2, 4), IsEqual.equalTo(9.0 / 32));
		Assert.assertThat(matrix.getAt(3, 4), IsEqual.equalTo(16.0 / 32)); // pre-trusted
		Assert.assertThat(matrix.getAt(4, 4), IsEqual.equalTo(1.0 / 32)); // self
	}

	//endregion

	private static class MockScoreProvider implements ScoreProvider {
		@Override
		public double calculateTrustScore(NodeExperience experience) {
			long numSuccessfulCalls = experience.successfulCalls().get();
			long numFailedCalls = experience.failedCalls().get();
			return (numFailedCalls * numSuccessfulCalls) * (numSuccessfulCalls + numFailedCalls);
		}

		@Override
		public double calculateCredibilityScore(final Node node1, final Node node2, final Node node3) {
			return 0;
		}
	}
}
