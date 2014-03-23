package org.nem.peer.trust;

import org.nem.peer.Node;

import java.util.Set;

/**
 * Represents information about pre-trusted nodes.
 */
public class PreTrustedNodes {

    private Set<Node> nodes;

    /**
     * Creates a pre-trusted nodes object.
     *
     * @param nodes The pre-trusted nodes.
     */
    public PreTrustedNodes(final Set<Node> nodes) {
        this.nodes = nodes;
    }

    /**
     * Gets the number of pre-trusted nodes.
     *
     * @return The number of pre-trusted nodes.
     */
    public int getNumPreTrustedNodes() {
        return this.nodes.size();
    }

    /**
     * Gets a value indicating whether or not the specified node is pre-trusted.
     *
     * @param node The node.
     * @return true if the node is pre-trusted.
     */
    public boolean isPreTrusted(final Node node) {
        return this.nodes.contains(node);
    }

    /**
     * Gets a vector of pre-trust values for all specified nodes.
     *
     * @param nodes The nodes.
     * @return A vector of pre-trust values.
     */
    public Vector getPreTrustVector(final Node[] nodes) {
        final int numPreTrustedNodes = this.getNumPreTrustedNodes();
        final Vector preTrustVector = new Vector(nodes.length);
        if (0 == numPreTrustedNodes) {
            preTrustVector.setAll(1.0 / nodes.length);
            return preTrustVector;
        }

        for (int i = 0; i < nodes.length; i++)
            preTrustVector.setAt(i, this.isPreTrusted(nodes[i]) ? 1.0 / numPreTrustedNodes : 0.0);

        return preTrustVector;
    }
}
