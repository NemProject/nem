package org.nem.peer.v2;

import java.util.logging.Logger;

/**
 * A node in the NEM network.
 * TODO: With the current structure this class isn't needed.
 * TODO: It probably makes sense to remove this one and rename NodeInfo -> Node.
 */
public class Node {

    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());

    private final NodeInfo info;
    private NodeStatus status;

    /**
     * Creates a new node.
     *
     * @param info Information about the node.
     */
    public Node(final NodeInfo info) {
        this.info = info;
        this.status = NodeStatus.INACTIVE;
    }

    /**
     * Gets information about the node.
     *
     * @return Information about the node.
     */
    public NodeInfo getInfo() { return this.info; }

    /***
     * Gets this node's status.
     *
     * @return This node's status.
     */
    public NodeStatus getStatus() { return this.status; }

    /**
     * Sets this node's status.
     *
     * @param status The desired status.
     */
    public void setStatus(final NodeStatus status) {
        LOGGER.info(String.format("%s changed to %s", this, status));
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Node %s (%s)", this.getInfo().getEndpoint().getBaseUrl().getHost(), this.status);
    }
}
