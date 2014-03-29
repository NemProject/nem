package org.nem.peer.test;

import org.nem.core.test.Utils;
import org.nem.peer.Node;
import org.nem.peer.trust.ScoreProvider;
import org.nem.peer.trust.score.NodeExperience;
import org.nem.peer.trust.score.TrustScores;

/**
 * Static class containing helper functions for testing ScoreProvider implementations.
 */
public class ScoreProviderTestUtils {

    /**
     * Calculates a trust score.
     *
     * @param provider The score provider.
     * @param successfulCalls The number of successful calls.
     * @param failedCalls The number of failed calls.
     * @return The trust score.
     */
    public static double calculateTrustScore(
        final ScoreProvider provider,
        final long successfulCalls,
        final long failedCalls) {
        // Arrange:
        final NodeExperience experience = new NodeExperience();
        experience.successfulCalls().set(successfulCalls);
        experience.failedCalls().set(failedCalls);

        // Act:
        return provider.calculateTrustScore(experience);
    }

    /**
     * Calculates a credibility score.
     *
     * @param provider The score provider.
     * @param trustScores The trust scores associated with the provider.
     * @param localTrust1 The local trust of the first node.
     * @param localTrustSum1 The local trust sum of the first node.
     * @param localTrust2 The local trust of the second node.
     * @param localTrustSum2 The local trust sum of the second node.
     * @return The credibility score.
     */
    public static double calculateCredibilityScore(
        final ScoreProvider provider,
        final TrustScores trustScores,
        final double localTrust1,
        final double localTrustSum1,
        final double localTrust2,
        final double localTrustSum2) {
        // Arrange:
        final Node node1 = Utils.createNodeWithPort(81);
        final Node node2 = Utils.createNodeWithPort(82);
        final Node node3 = Utils.createNodeWithPort(83);

        trustScores.getScore(node1, node3).score().set(localTrust1);
        trustScores.getScoreWeight(node1).set(localTrustSum1);

        trustScores.getScore(node2, node3).score().set(localTrust2);
        trustScores.getScoreWeight(node2).set(localTrustSum2);

        // Act:
        return provider.calculateCredibilityScore(node1, node2, node3);
    }
}
