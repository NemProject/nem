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
}
