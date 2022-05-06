package org.nem.peer.test;

import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;
import org.nem.peer.trust.ScoreProvider;
import org.nem.peer.trust.score.*;

/**
 * A test wrapper around a ScoreProvider.
 */
public class ScoreProviderTestContext {

	private final ScoreProvider scoreProvider;
	private final TrustScores trustScores;

	/**
	 * Creates a new test ScoreProvider context.
	 *
	 * @param scoreProvider The score provider.
	 */
	public ScoreProviderTestContext(final ScoreProvider scoreProvider) {
		this(scoreProvider, null);
	}

	/**
	 * Creates a new test ScoreProvider context.
	 *
	 * @param scoreProvider The score provider.
	 * @param trustScores The trust scores associated with the provider.
	 */
	public ScoreProviderTestContext(final ScoreProvider scoreProvider, final TrustScores trustScores) {
		this.scoreProvider = scoreProvider;
		this.trustScores = trustScores;
	}

	/**
	 * Calculates a trust score given the call counts.
	 *
	 * @param successfulCalls The number of successful calls.
	 * @param failedCalls The number of failed calls.
	 * @return The trust score.
	 */
	public double calculateTrustScore(final long successfulCalls, final long failedCalls) {
		// Arrange:
		final NodeExperience experience = new NodeExperience();
		experience.successfulCalls().set(successfulCalls);
		experience.failedCalls().set(failedCalls);

		// Act:
		return this.scoreProvider.calculateTrustScore(experience);
	}

	/**
	 * Calculates a credibility score.
	 *
	 * @param localTrust1 The local trust of the first node.
	 * @param localTrustSum1 The local trust sum of the first node.
	 * @param localTrust2 The local trust of the second node.
	 * @param localTrustSum2 The local trust sum of the second node.
	 * @return The credibility score.
	 */
	public double calculateCredibilityScore(final double localTrust1, final double localTrustSum1, final double localTrust2,
			final double localTrustSum2) {
		// Arrange:
		final Node node1 = NodeUtils.createNodeWithName("a");
		final Node node2 = NodeUtils.createNodeWithName("b");
		final Node node3 = NodeUtils.createNodeWithName("c");

		this.trustScores.getScore(node1, node3).score().set(localTrust1);
		this.trustScores.getScoreWeight(node1).set(localTrustSum1);

		this.trustScores.getScore(node2, node3).score().set(localTrust2);
		this.trustScores.getScoreWeight(node2).set(localTrustSum2);

		// Act:
		return this.scoreProvider.calculateCredibilityScore(node1, node2, node3);
	}
}
