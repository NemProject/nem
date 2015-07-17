package org.nem.core.model;

import java.util.*;

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
	private static final int MOSAIC_TYPE = 0x4000;
	private static final int SMART_TILES_TYPE = 0x8000;

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

	/**
	 * A mosaic creation transaction.
	 */
	public static final int MOSAIC_CREATION = MOSAIC_TYPE | 0x01;

	/**
	 * A smart tiles supply change transaction.
	 */
	public static final int SMART_TILE_SUPPLY_CHANGE = SMART_TILES_TYPE | 0x01;

	/**
	 * Gets all active types.
	 *
	 * @return The types.
	 */
	public static Collection<Integer> getActiveTypes() {
		final List<Integer> types = new ArrayList<>(getBlockEmbeddableTypes());
		types.add(MULTISIG_SIGNATURE);
		return types;
	}

	/**
	 * Gets all block embeddable types.
	 *
	 * @return The parameters
	 */
	public static Collection<Integer> getBlockEmbeddableTypes() {
		final List<Integer> types = new ArrayList<>(getMultisigEmbeddableTypes());
		types.add(MULTISIG);
		return types;
	}

	/**
	 * Gets all multisig embeddable types.
	 *
	 * @return The types.
	 */
	public static Collection<Integer> getMultisigEmbeddableTypes() {
		return Arrays.asList(
				TRANSFER,
				IMPORTANCE_TRANSFER,
				MULTISIG_AGGREGATE_MODIFICATION,
				PROVISION_NAMESPACE,
				MOSAIC_CREATION,
				SMART_TILE_SUPPLY_CHANGE);
	}
}