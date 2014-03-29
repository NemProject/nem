package org.nem.peer.trust;

import org.nem.peer.Node;
import org.nem.peer.NodeCollection;
import org.nem.peer.trust.score.NodeExperiences;

/**
 * Contains contextual information that can be used to influence the trust computation.
 *
 * TODO: the intent is for this class to contain the trusting procedure which is configurable by other classes.
 * TODO: test this class (maybe)
 */
public class TrustContext {

    /**
     * Error margin in convergence tests.
     */
    private static final double EPSILON = 0.0001;

    /**
     * Maximal number of iterations when computing the global trust vector.
     */
    private static final int MAX_ITERATIONS = 10;

    /**
     * Weighting constant for the pre-trusted peers.
     */
    private static final double ALPHA = 0.05;

    private final Node[] nodes;
    private final Node localNode;
    private final NodeExperiences nodeExperiences;
    private final PreTrustedNodes preTrustedNodes;
    private final TrustProvider trustProvider;

    /**
     * Creates a new trust context.
     *
     * @param nodes The known nodes.
     * @param localNode The local nodes
     * @param nodeExperiences Node experiences information.
     * @param preTrustedNodes Pre-trusted node information.
     * @param trustProvider The trust provider to use.
     */
    public TrustContext(
        final NodeCollection nodes,
        final Node localNode,
        final NodeExperiences nodeExperiences,
        final PreTrustedNodes preTrustedNodes,
        final TrustProvider trustProvider) {

        this.nodes = TrustUtils.toNodeArray(nodes, localNode);
        this.localNode = localNode;
        this.nodeExperiences = nodeExperiences;
        this.preTrustedNodes = preTrustedNodes;
        this.trustProvider = trustProvider;
    }

    /**
     * Gets all nodes in the trust context.
     *
     * @return All nodes.
     */
    public Node[] getNodes() { return this.nodes; }

    /**
     * Gets the local node
     *
     * @return The local noe.
     */
    public Node getLocalNode() { return this.localNode; }

    /**
     * Gets all node experience information from the trust context.
     *
     * @return Node experience information.
     */
    public NodeExperiences getNodeExperiences() { return this.nodeExperiences; }

    /**
     * Gets all pre-trusted node information from the trust context.
     *
     * @return Pre-trusted node information.
     */
    public PreTrustedNodes getPreTrustedNodes() { return this.preTrustedNodes; }

    /**
     * Gets a vector of pre-trust values for all nodes.
     *
     * @return A vector of pre-trust values.
     */
    public Vector getPreTrustVector() {
        return this.preTrustedNodes.getPreTrustVector(this.nodes);
    }

    /**
     * Gets a trust matrix of local trust values weighted with credibility for all nodes.
     * Matrix(r, c) contains the local experience that c has with r.
     *
     * @return A transposed matrix of local trust values.
     */
    public Matrix getTrustMatrix() {
        final Matrix matrix = this.nodeExperiences.getTrustMatrix(this.nodes);
        matrix.normalizeColumns();
        return matrix;
    }

    /**
     * Computes the global trust values based on the current information.
     *
     * @return The global trust values based on the current information.
     */
    public Vector compute() {
        // (1) compute the trust we have in other nodes due to our own experience
        this.updateLocalTrust(this.localNode);

        // (2) normalize each node's local trust values
        this.nodeExperiences.normalizeLocalTrust(this.nodes);

        // (3) compute the feedback credibility
        this.updateFeedbackCredibility(this.localNode);

        // (4) Update the global trust
        return computeGlobalTrust();
    }

    /**
     * Simulates a step by all non-local nodes in the system.
     */
    public void simulate() {
        for (final Node node : this.nodes) {
            if (node.equals(this.localNode))
                continue;

            this.updateLocalTrust(node);
            this.updateFeedbackCredibility(node);
        }
    }

    private void updateLocalTrust(final Node node) {
        double localTrustSum = TrustUtils.updateLocalTrust(
            node,
            this.nodes,
            this.nodeExperiences,
            this.preTrustedNodes,
            this.trustProvider);

        this.nodeExperiences.getNodeExperience(node, node).localTrustSum().set(localTrustSum);
    }

    private void updateFeedbackCredibility(final Node node) {
        final Vector vector = this.nodeExperiences.calculateFeedbackCredibilityVector(node, this.nodes, this.trustProvider);
        this.nodeExperiences.setFeedbackCredibilityVector(node, this.nodes, vector);
    }

    private Vector computeGlobalTrust() {
        final EigenTrustPowerIterator iterator = new EigenTrustPowerIterator(
            this.getPreTrustVector(),
            this.getTrustMatrix(),
            MAX_ITERATIONS,
            ALPHA,
            EPSILON);
        iterator.run();
        return iterator.getResult();
    }
}
