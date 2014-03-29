package org.nem.peer.trust.score;

import org.nem.peer.Node;
import org.nem.peer.trust.Matrix;
import org.nem.peer.trust.TrustProvider;
import org.nem.peer.trust.Vector;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains experiences for a set of nodes.
 */
public class NodeExperiences {

    private final Map<Node, Map<Node, NodeExperience>> nodeExperiences = new ConcurrentHashMap<>();

    /**
     * Gets the NodeExperience source has with peer.
     *
     * @param source The node reporting experience.
     * @param peer The node being reported about.
     * @return The experience source has with peer.
     */
    public NodeExperience getNodeExperience(final Node source, final Node peer) {
        final Map<Node, NodeExperience> localExperiences = this.getNodeExperiences(source);

        NodeExperience experience = localExperiences.get(peer);
        if (null == experience) {
            experience = new NodeExperience();
            localExperiences.put(peer, experience);
        }

        return experience;
    }

    private Map<Node, NodeExperience> getNodeExperiences(final Node source) {
        Map<Node, NodeExperience> localExperiences = this.nodeExperiences.get(source);
        if (null == localExperiences) {
            localExperiences = new ConcurrentHashMap<>();
            this.nodeExperiences.put(source, localExperiences);
        }

        return localExperiences;
    }

    /**
     * Gets a transposed matrix of local trust values weighted with credibility for all specified nodes.
     * Matrix(r, c) contains the local experience that c has with r.
     *
     * @param nodes The nodes.
     * @return A transposed matrix of local trust values.
     */
    public Matrix getTrustMatrix(final Node[] nodes) {
        final int numNodes = nodes.length;
        final Matrix trustMatrix = new Matrix(numNodes, numNodes);
        for (int i = 0; i < numNodes; ++i) {
            for (int j = 0; j < numNodes; ++j) {
                final NodeExperience experience = this.getNodeExperience(nodes[i], nodes[j]);
                trustMatrix.setAt(j, i, experience.localTrust().get() * experience.feedbackCredibility().get());
            }
        }

        return trustMatrix;
    }

    /**
     * Gets a local trust vector that contains the local trust node has with
     * each node in nodes.
     *
     * @param node The node.
     * @param nodes The other nodes.
     * @return A local trust vector.
     */
    public Vector getLocalTrustVector(final Node node, final Node[] nodes) {
        final Vector vector = new Vector(nodes.length);
        for (int i = 0; i < nodes.length; ++i) {
            final NodeExperience experience = this.getNodeExperience(node, nodes[i]);
            vector.setAt(i, experience.localTrust().get());
        }

        return vector;
    }

    /**
     * Sets the local trust that node has with each node in nodes using the specified vector.
     *
     * @param node The node.
     * @param nodes The other nodes.
     * @param trustVector The trust values.
     */
    public void setLocalTrustVector(final Node node, final Node[] nodes, final Vector trustVector) {
        if (nodes.length != trustVector.getSize())
            throw new InvalidParameterException("nodes and trustVector must be same size");

        for (int i = 0; i < nodes.length; ++i) {
            final NodeExperience experience = this.getNodeExperience(node, nodes[i]);
            experience.localTrust().set(trustVector.getAt(i));
        }
    }

    /**
     * Normalizes the local trust values so that the sum of a node's local trust experiences is 1.
     *
     * @param nodes The nodes that should have their trust values normalized.
     */
    public void normalizeLocalTrust(final Node[] nodes) {
        for (final Node node : nodes) {
            final Vector vector = this.getLocalTrustVector(node, nodes);
            vector.normalize();
            this.setLocalTrustVector(node, nodes, vector);
        }
    }

    /**
     * Gets a shared experience matrix.
     * Matrix(r, c) contains 1 if node and node(r) have both interacted with node(c).
     *
     * @param node The node.
     * @param nodes The other nodes.
     * @return The shared experiences matrix.
     */
    public Matrix getSharedExperienceMatrix(final Node node, final Node[] nodes) {
        final Matrix matrix = new Matrix(nodes.length, nodes.length);
        for (int i = 0; i < nodes.length; ++i) {
            // skip the node (since the node will not have interacted with itself)
            if (node.equals(nodes[i]))
                continue;

            for (int j = 0; j < nodes.length; ++j) {
                if (nodes[i].equals(nodes[j]))
                    continue;

                final NodeExperience experience1 = this.getNodeExperience(node, nodes[j]);
                final NodeExperience experience2 = this.getNodeExperience(nodes[i], nodes[j]);
                if (experience1.totalCalls() > 0 && experience2.totalCalls() > 0)
                    matrix.setAt(i, j, 1.0);
            }
        }

        return matrix;
    }

    /**
     * Calculates a feedback credibility vector.
     *
     * @param node The node.
     * @param nodes The other nodes.
     * @param trustProvider The trust provider.
     * @return The feedback credibility vector.
     */
    public Vector calculateFeedbackCredibilityVector(final Node node, final Node[] nodes, final TrustProvider trustProvider) {

        Matrix sharedExperiencesMatrix = this.getSharedExperienceMatrix(node, nodes);

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

                final NodeExperience experience1 = this.getNodeExperience(node, nodes[j]);
                final NodeExperience experience2 = this.getNodeExperience(nodes[i], nodes[j]);
                double score = trustProvider.calculateCredibilityScore(experience1, experience2);
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


    /**
     * Gets a feedback credibility vector that contains the feedback credibility node has with
     * each node in nodes.
     *
     * @param node The node.
     * @param nodes The other nodes.
     * @return A local trust vector.
     */
    public Vector getFeedbackCredibilityVector(final Node node, final Node[] nodes) {
        final Vector vector = new Vector(nodes.length);
        for (int i = 0; i < nodes.length; ++i) {
            final NodeExperience experience = this.getNodeExperience(node, nodes[i]);
            vector.setAt(i, experience.feedbackCredibility().get());
        }

        return vector;
    }

    /**
     * Sets the feedback credibility that node has with each node in nodes using the specified vector.
     *
     * @param node The node.
     * @param nodes The other nodes.
     * @param feedbackCredibilityVector The credibility values.
     */
    public void setFeedbackCredibilityVector(final Node node, final Node[] nodes, final Vector feedbackCredibilityVector) {
        if (nodes.length != feedbackCredibilityVector.getSize())
            throw new InvalidParameterException("nodes and trustVector must be same size");

        for (int i = 0; i < nodes.length; ++i) {
            final NodeExperience experience = this.getNodeExperience(node, nodes[i]);
            experience.feedbackCredibility().set(feedbackCredibilityVector.getAt(i));
        }
    }
}