package org.nem.peer.v2;

/**
 * Possible statuses of a NEM node.
 */
public enum NodeStatus {
    /**
     * The node is connected.
     */
    ACTIVE,

    /**
     * The node is not connected.
     */
    INACTIVE,

    /**
     * The node is not a NEM node.
     */
    FAILURE
}