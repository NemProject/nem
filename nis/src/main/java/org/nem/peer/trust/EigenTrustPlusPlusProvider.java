package org.nem.peer.trust;

import org.nem.peer.Node;
import org.nem.peer.trust.score.*;

/**
 * Trust provider based on the EigenTrust algorithm.
 */
public class EigenTrustPlusPlusProvider extends EigenTrustProvider {

    private final CredibilityScores credibilityScores = new CredibilityScores();

    @Override
    public double calculateTrustScore(final NodeExperience experience) {
        return experience.successfulCalls().get();
    }

    @Override
    public double calculateCredibilityScore(final NodeExperience experience1, final NodeExperience experience2) {
        return 1; // TODO: this needs to be fixed
//        return experience1.localTrust().get() * experience1.localTrustSum().get() - experience2.localTrust().get() * experience2.localTrustSum().get();
    }

    // TODO: test
    @Override
    public Matrix getTrustMatrix(final Node[] nodes) {
        final Matrix trustMatrix = this.getTrustScores().getScoreMatrix(nodes);
        final Matrix credibilityMatrix = this.credibilityScores.getScoreMatrix(nodes);
        final Matrix matrix = trustMatrix.multiplyElementWise(credibilityMatrix);
        matrix.normalizeColumns();
        return matrix;
    }

    /**
     * Calculates a feedback credibility vector.
     *
     * @param node The node.
     * @param nodes The other nodes.
     * @param nodeExperiences The node experiences.
     * @return The feedback credibility vector.
     */
    public Vector calculateFeedbackCredibilityVector(final Node node, final Node[] nodes, final NodeExperiences nodeExperiences) {
        final Matrix sharedExperiencesMatrix = nodeExperiences.getSharedExperienceMatrix(node, nodes);

        final Vector vector = new Vector(nodes.length);
        for (int i = 0; i < nodes.length; ++i) {
            if (node.equals(nodes[i])) {
                // the node should completely trust itself
                vector.setAt(i, 1);
                continue;
            }

            double sum = 0.0;
            int numCommonPartners = 0;
            for (int j = 0; j < nodes.length; ++j) {
                if (0 == sharedExperiencesMatrix.getAt(i, j))
                    continue;

                final NodeExperience experience1 = nodeExperiences.getNodeExperience(node, nodes[j]);
                final NodeExperience experience2 = nodeExperiences.getNodeExperience(nodes[i], nodes[j]);
                double score = this.calculateCredibilityScore(experience1, experience2);
                sum += score * score;
                ++numCommonPartners;
            }

            if (0 == numCommonPartners)
                continue;

            // Original paper suggests sim = 1 - Math.sqrt(sum).
            // This leads to values of around 0.5 for evil nodes and almost 1 for honest nodes.
            // We get better results by taking a power of that value since (0.5)^n quickly converges to 0 for increasing n.
            // The value n=4 is just an example which works well.
            sum /= numCommonPartners;
            vector.setAt(i, Math.pow(1 - Math.sqrt(sum), 4));
        }

        return vector;
    }
}