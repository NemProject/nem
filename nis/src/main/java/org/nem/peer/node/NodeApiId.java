package org.nem.peer.node;

/**
 * IDs of node-related APIs.
 */
public enum NodeApiId {

	//region block/*

	/**
	 * The block/at API.
	 */
	REST_BLOCK_AT,

	//endregion

	//region chain/*

	/**
	 * The chain/blocks-after API.
	 */
	REST_CHAIN_BLOCKS_AFTER,

	/**
	 * The chain/hashes-from API
	 */
	REST_CHAIN_HASHES_FROM,

	/**
	 * The chain/last-block API.
	 */
	REST_CHAIN_LAST_BLOCK,

	//endregion

	//region node/*

	/**
	 * The node/cysm API.
	 */
	REST_NODE_CAN_YOU_SEE_ME,

	/**
	 * The node/extended-info API.
	 */
	REST_NODE_EXTENDED_INFO,

	/**
	 * The node/info API.
	 */
	REST_NODE_INFO,

	/**
	 * The node/peer-list/all API.
	 */
	REST_NODE_PEER_LIST,

	/**
	 * The node/peer-list/active API.
	 */
	REST_NODE_PEER_LIST_ACTIVE,

	/**
	 * The node/ping API.
	 */
	REST_NODE_PING,

	//endregion

	//region push/*

	/**
	 * The push/block API.
	 */
	REST_PUSH_BLOCK,

	/**
	 * The push/transaction API.
	 */
	REST_PUSH_TRANSACTION,

	//endregion
}
