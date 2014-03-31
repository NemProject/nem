package org.nem.peer.test;

import org.nem.core.test.Utils;
import org.nem.peer.Node;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.util.HashSet;
import java.util.Set;

/**
 * A test wrapper around a TrustContext.
 */
public class TestTrustContext {

    private final Node localNode;
    private final Node[] nodes;
    private final NodeExperiences nodeExperiences;
    private final PreTrustedNodes preTrustedNodes;

    /**
     * Creates a new test trust context.
     */
    public TestTrustContext() {
        this.localNode = Utils.createNodeWithPort(80);
        this.nodes = new Node[] {
            Utils.createNodeWithPort(81),
            Utils.createNodeWithPort(87),
            Utils.createNodeWithPort(86),
            Utils.createNodeWithPort(89),
            localNode
        };

        this.nodeExperiences = new NodeExperiences();

        Set<Node> preTrustedNodeSet = new HashSet<>();
        preTrustedNodeSet.add(this.nodes[0]);
        preTrustedNodeSet.add(this.nodes[3]);
        this.preTrustedNodes = new PreTrustedNodes(preTrustedNodeSet);
    }

    /**
     * Gets the real trust context.
     *
     * @return The trust context.
     */
    public TrustContext getContext() {
        return new TrustContext(
            this.nodes,
            this.localNode,
            this.nodeExperiences,
            this.preTrustedNodes,
            new TrustParameters());
    }

    /**
     * Sets the number of successful and failed calls that the local node has had with
     * the node at the specified index.
     *
     * @param nodeIndex The node index.
     * @param numSuccessfulCalls The number of successful calls.
     * @param numFailedCalls The number of failed calls.
     */
    public void setCallCounts(final int nodeIndex, final int numSuccessfulCalls, final int numFailedCalls) {
        final NodeExperience experience = this.nodeExperiences.getNodeExperience(this.localNode, this.nodes[nodeIndex]);
        experience.successfulCalls().set(numSuccessfulCalls);
        experience.failedCalls().set(numFailedCalls);
    }
}
