package org.nem.peer.v2;

/**
 * IDs of node-related APIs.
 * TODO: consider changing these to strings.
 */
public enum NodeApiId {
    /**
     * The node/info API.
     */
    REST_NODE_INFO,

    /**
     * The peer/new API.
     */
    REST_ADD_PEER,

    /**
     * The node/peer-list API.
     */
    REST_NODE_PEER_LIST,

    /**
     * The chain API.
     */
    REST_CHAIN
}
