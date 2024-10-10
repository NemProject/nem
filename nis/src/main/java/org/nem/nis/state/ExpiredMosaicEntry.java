package org.nem.nis.state;

import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;

/**
 * An immutable expired mosaic entry.
 */
public class ExpiredMosaicEntry {
	private final MosaicId mosaicId;
	private final ReadOnlyMosaicBalances mosaicBalances;
	private final ExpiredMosaicType expiredMosaicType;

	/**
	 * Creates a new expired mosaic entry.
	 *
	 * @param mosaicId Mosaic id.
	 * @param mosaicBalances Mosaic balances.
	 * @param expiredMosaicType Expired mosaic type.
	 */
	public ExpiredMosaicEntry(final MosaicId mosaicId, final ReadOnlyMosaicBalances mosaicBalances,
			final ExpiredMosaicType expiredMosaicType) {
		this.mosaicId = mosaicId;
		this.mosaicBalances = mosaicBalances;
		this.expiredMosaicType = expiredMosaicType;
	}

	/**
	 * Gets the mosaic id.
	 *
	 * @return Mosaic id.
	 */
	public MosaicId getMosaicId() {
		return this.mosaicId;
	}

	/**
	 * Gets the mosaic balances.
	 *
	 * @return Mosaic balances.
	 */
	public ReadOnlyMosaicBalances getBalances() {
		return this.mosaicBalances;
	}

	/**
	 * Gets the expired mosaic type.
	 *
	 * @return Expired mosaic type.
	 */
	public ExpiredMosaicType getExpiredMosaicType() {
		return this.expiredMosaicType;
	}
}
