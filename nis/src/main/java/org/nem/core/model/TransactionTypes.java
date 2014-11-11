package org.nem.core.model;

/**
 * Static class containing transaction type constants.
 */
public class TransactionTypes {
	private static final int TRANSFER_TYPE = 0x0100;
	private static final int ASSET_TYPE = 0x0200;
	private static final int SNAPSHOT_TYPE = 0x0400;
	private static final int IMPORTANCE_TYPE = 0x0800;
	private static final int MULTISIG_TYPE = 0x1000;

	/**
	 * A transfer transaction.
	 */
	public static final int TRANSFER = TRANSFER_TYPE | 0x01;

	/**
	 * Importance transfer transaction.
	 */
	public static final int IMPORTANCE_TRANSFER = IMPORTANCE_TYPE | 0x01;

	/**
	 * A new asset transaction.
	 */
	public static final int ASSET_NEW = ASSET_TYPE | 0x01;

	/**
	 * An asset ask transaction.
	 */
	public static final int ASSET_ASK = ASSET_TYPE | 0x02;

	/**
	 * An asset bid transaction.
	 */
	public static final int ASSET_BID = ASSET_TYPE | 0x03;

	/**
	 * A snapshot transaction.
	 */
	public static final int SNAPSHOT = SNAPSHOT_TYPE | 0x01;

	/**
	 * Announce account as multi-sig.
	 */
	public static final int MULTISIG_MODIFY_SIGNER = MULTISIG_TYPE | 0x02;

	/**
	 * Multisig signature
	 */
	public static final int MULTISIG_SIGNATURE = MULTISIG_TYPE | 0x03;

}