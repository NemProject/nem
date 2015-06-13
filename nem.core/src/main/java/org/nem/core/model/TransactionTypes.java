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
	private static final int NAMESPACE_TYPE = 0x2000;

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
	 * A multisig change transaction (e.g. announce an account as multi-sig).
	 */
	public static final int MULTISIG_AGGREGATE_MODIFICATION = MULTISIG_TYPE | 0x01;

	/**
	 * A multisig signature transaction.
	 */
	public static final int MULTISIG_SIGNATURE = MULTISIG_TYPE | 0x02;

	/**
	 * A multisig transaction.
	 */
	public static final int MULTISIG = MULTISIG_TYPE | 0x04;

	/**
	 * A provision namespace transaction.
	 */
	public static final int PROVISION_NAMESPACE = NAMESPACE_TYPE | 0x01;
}