package org.nem.peer;

/**
 * IDs of node-related APIs.
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
	 * Transaction announcement
	 */
	REST_PUSH_TRANSACTION,

    /**
     * The chain API.
     */
    REST_CHAIN
}
