package org.nem.nis.cache;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.ReadOnlyMosaicBalances;

/**
 * An expired mosaic cache containing information about mosaics that were zeroed upon expiration.
 */
public interface ExpiredMosaicCache extends ReadOnlyExpiredMosaicCache {
	/**
	 * Adds a mosaic expiration.
	 *
	 * @param height Height of expiration.
	 * @param mosaicId Id of expiring mosaic.
	 * @param balances Expiring mosaic balances.
	 */
	void addExpiration(final BlockHeight height, final MosaicId mosaicId, final ReadOnlyMosaicBalances balances);

	/**
	 * Removes all expirations at the specified height.
	 *
	 * @param height Height of expiration.
	 */
	void removeAll(final BlockHeight height);
}
