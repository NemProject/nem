package org.nem.core.connect.client;

/**
 * NIS REST API paths.
 *
 * TODO 20140909 J-B: i think i like what you did in NCC better regarding enum -> path mappings
 * (this is just a more general comment because i'd eventually like to have something similar in NIS, but not necessarily now)
 * TODO 20140910 BR -> J I don't understand, which enum -> path mapping in NCC do you mean? (this file is part of NIS)
 */
public enum NisApiId {

	//region account/*

	/**
	 * The /account/unlock API
	 */
	NIS_REST_ACCOUNT_UNLOCK("/account/unlock"),

	/**
	 * The /account/lock API
	 */
	NIS_REST_ACCOUNT_LOCK("/account/lock"),

	/**
	 * The /account/harvests API
	 */
	NIS_REST_ACCOUNT_HARVESTS("/account/harvests"),

	/**
	 * The /account/get API
	 */
	NIS_REST_ACCOUNT_LOOK_UP("/account/get"),

	/**
	 * The /account/transfers/all API
	 */
	NIS_REST_ACCOUNT_TRANSFERS_ALL("/account/transfers/all"),

	/**
	 * The /account/transfers/incoming API
	 */
	NIS_REST_ACCOUNT_TRANSFERS_INCOMING("/account/transfers/incoming"),

	/**
	 * The /account/transfers/outgoing API
	 */
	NIS_REST_ACCOUNT_TRANSFERS_OUTGOING("/account/transfers/outgoing"),

	/**
	 * The /account/transfers API
	 */
	NIS_REST_ACCOUNT_TRANSFERS("/account/transfers"),

	/**
	 * The /account/unconfirmedTransactions API
	 */
	NIS_REST_ACCOUNT_UNCONFIRMED("/account/unconfirmedTransactions"),

	//endregion

	//region chain/*

	/**
	 * The chain/last-block API.
	 */
	NIS_REST_CHAIN_LAST_BLOCK("/chain/last-block"),

	//endregion

	//region node/*

	/**
	 * The node/extended-info API.
	 */
	NIS_REST_NODE_EXTENDED_INFO("/node/extended-info"),

	/**
	 * The node/info API.
	 */
	NIS_REST_NODE_INFO("/node/info"),

	/**
	 * The node/peer-list/all API.
	 */
	NIS_REST_PEER_LIST("/node/peer-list/all"),

	/**
	 * The node/peer-list/active API.
	 */
	NIS_REST_PEER_LIST_ACTIVE("/node/peer-list/active"),

	/**
	 * The /node/boot API.
	 */
	NIS_REST_NODE_BOOT("/node/boot"),

	//endregion

	//region transaction/*

	/**
	 * The /transaction/announce API.
	 */
	NIS_REST_TRANSACTION_ANNOUNCE("/transaction/announce"),

	//endregion

	// region /*

	/**
	 * The /heartbeat API.
	 */
	NIS_REST_HEARTBEAT("/heartbeat"),

	/**
	 * The /shutdown API.
	 */
	NIS_REST_SHUTDOWN("/shutdown"),

	/**
	 * The /status API.
	 */
	NIS_REST_STATUS("/status");

	//endregion

	private final String value;

	/**
	 * Creates a NIS API id.
	 *
	 * @param value The string representation.
	 */
	private NisApiId(final String value) {
		this.value = value;
	}

	/**
	 * Gets the underlying string.
	 *
	 * @return The API id string.
	 */
	public String toString() {
		return this.value;
	}
}