package org.nem.peer.trust;

import org.nem.peer.Node;
import org.nem.peer.trust.score.NodeExperience;
import org.nem.peer.trust.score.TrustScores;

/**
 * EigenTrust algorithm implementation.
 */
public class EigenTrust {

    private final TrustScores trustScores;
    private final org.nem.peer.trust.ScoreProvider scoreProvider;

    /**
     * Creates a new eigen trust object using the default score provider.
     */
    public EigenTrust() {
        this(new ScoreProvider());
    }

    /**
     * Creates a new eigen trust object using a custom score provider.
     *
     * @param scoreProvider The score provider to use.
     */
    public EigenTrust(final org.nem.peer.trust.ScoreProvider scoreProvider) {
        this.trustScores = new TrustScores();
        this.scoreProvider = scoreProvider;
    }

    /**
     * Gets the score provider.
     *
     * @return the score provider.
     */
    protected org.nem.peer.trust.ScoreProvider getScoreProvider() {
        return this.scoreProvider;
    }

    /**
     * Returns the trust matrix for the specified nodes.
     *
     * @param nodes The nodes.
     * @return The trust matrix
     */
    public Matrix getTrustMatrix(final Node[] nodes) {
        final Matrix matrix = this.trustScores.getScoreMatrix(nodes);
        matrix.normalizeColumns();
        return matrix;
    }

    /**
     * Gets the trust scores.
     *
     * @return The trust scores.
     */
    public TrustScores getTrustScores() { return this.trustScores; }

    /**
     * Updates the local trust values for the specified node using the specified context.
     *
     * @param node The node.
     * @param context The trust context.
     */
    public void updateLocalTrust(final Node node, final TrustContext context) {
        int index = 0;
        final Node[] nodes = context.getNodes();
        final Vector scoreVector = new Vector(nodes.length);
        for (final Node otherNode : nodes) {
            final NodeExperience experience = context.getNodeExperiences().getNodeExperience(node, otherNode);
            final long successfulCalls = experience.successfulCalls().get();
            final long failedCalls = experience.failedCalls().get();
            final double totalCalls = successfulCalls + failedCalls;

            double score;
            if (totalCalls > 0)
                score = this.scoreProvider.calculateTrustScore(experience)/totalCalls;
            else
                score = context.getPreTrustedNodes().isPreTrusted(otherNode) || node.equals(otherNode) ? 1.0 : 0.0;

            scoreVector.setAt(index++, score);
        }

        double scoreWeight = scoreVector.sum();
        scoreVector.normalize();
        this.trustScores.setScoreVector(node, nodes, scoreVector);
        this.trustScores.getScoreWeight(node).set(scoreWeight);
    }

    /**
     * An EigenTrust score provider implementation.
     */
    public static class ScoreProvider implements org.nem.peer.trust.ScoreProvider {

        @Override
        public double calculateTrustScore(final NodeExperience experience) {
            return Math.max(experience.successfulCalls().get() - experience.failedCalls().get(), 0.0);
        }

        @Override
        public double calculateCredibilityScore(final Node node1, final Node node2, final Node node3) {
            return 0;
        }
    }
}