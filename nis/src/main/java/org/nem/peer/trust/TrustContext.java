package org.nem.peer.trust;

import org.nem.peer.Node;
import org.nem.peer.trust.score.*;

/**
 * Contains contextual information that can be used to influence the trust computation.
 */
public class TrustContext {

    private final Node[] nodes;
    private final Node localNode;
    private final NodeExperiences nodeExperiences;
    private final PreTrustedNodes preTrustedNodes;

    /**
     * Creates a new trust context.
     *
     * @param nodes The known nodes (including the local node).
     * @param localNode The local node.
     * @param nodeExperiences Node experiences information.
     * @param preTrustedNodes Pre-trusted node information.
     */
    public TrustContext(
        final Node[] nodes,
        final Node localNode,
        final NodeExperiences nodeExperiences,
        final PreTrustedNodes preTrustedNodes) {

        this.nodes = nodes;
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
}
