package org.nem.core.model;

/**
 * Common place to have BlockChain-related constants accessible from all modules.
 */
public class BlockChainConstants {

	/**
	 * The maximum number of seconds in the future that an entity's timestamp can be
	 * without the entity being rejected.
	 */
	public static final int MAX_ALLOWED_SECONDS_AHEAD_OF_TIME = 10;

	/**
	 * The maximum number of cosignatories that a multisig account can have.
	 */
	public static final int MAX_ALLOWED_COSIGNATORIES_PER_ACCOUNT = 32;

	/**
	 * The maximum number of mosaics allowed in a transfer transaction.
	 */
	public static final int MAX_ALLOWED_MOSAICS_PER_TRANSFER = 10;
}
