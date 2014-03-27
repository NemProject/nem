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
	 * Transaction announcement.
	 */
	REST_PUSH_TRANSACTION,

	/**
	 * Block announcement.
	 */
	REST_PUSH_BLOCK,

    /**
     * The chain API - last block.
     */
    REST_CHAIN_LAST_BLOCK,

	/**
	 * The chain API - block at height
	 */
	REST_CHAIN_BLOCK_AT
}
