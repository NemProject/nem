package org.nem.core.model;

/**
 * Static class containing transaction type constants.
 */
public class TransactionTypes {
	// currently transactions inside blocks are sorted by type,
	// so changing those IDs might break a LOT of things...
	// TODO 20140909 J-G: should we prioritize different *types* of transactions (i.e. is importance more important than transfer)?
	// by type, not by size, I don't know what I was thinking about when typing that comment, does your comment still apply?
	// prioritizing by type is probably not the best idea, but it will make mapping blocks a lot easier :/
	private static final int TRANSFER_TYPE = 0x0100;
	private static final int ASSET_TYPE = 0x0200;
	private static final int SNAPSHOT_TYPE = 0x0400;
	private static final int IMPORTANCE_TYPE = 0x0800;

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
	public static final int SNAPSHOT = SNAPSHOT_TYPE | 0x00;
}