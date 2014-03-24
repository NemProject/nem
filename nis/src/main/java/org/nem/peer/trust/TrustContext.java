package org.nem.peer.trust;

import org.nem.peer.Node;
import org.nem.peer.NodeCollection;

/**
 * Contains contextual information that can be used to influence the trust computation.
 *
 * TODO: the intent is for this class to contain the trusting procedure which is configurable by other classes.
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

    /**
     * Creates a new trust context.
     *
     *
     */
    public TrustContext(
        final NodeCollection nodes,
        final Node localNode,
        final NodeExperiences nodeExperiences,
        final PreTrustedNodes preTrustedNodes) {

        this.nodes = TrustUtils.toNodeArray(nodes, localNode);
        this.localNode = localNode;
        this.nodeExperiences = nodeExperiences;
        this.preTrustedNodes = preTrustedNodes;
    }

    /**
     * Gets all nodes in the trust context.
     *
     * @return All nodes.
     */
    public Node[] getNodes() { return this.nodes; }

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

    public Vector compute() {
        // TODO: (1) compute the trust we have in other nodes due to our own experience
        //computeLocalTrust(peers[peers.length-1]);

        // (2) normalize each node's local trust values
        this.nodeExperiences.normalizeLocalTrust(this.nodes);

        // TODO: ETPP
        // computeFeedbackCredibility(peers[peers.length-1], peers);

        // Compute pre-trust vector
        final Vector preTrustVector = this.getPreTrustVector();

        // Set up the transpose of the trust matrix
        final Matrix trustMatrix = this.getTrustMatrix();

        // Calculate global trust
        return computeGlobalTrust();
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
