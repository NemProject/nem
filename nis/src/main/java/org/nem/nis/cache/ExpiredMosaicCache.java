package org.nem.nis.cache;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.*;

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
	 * @param expirationType Type of expiration.
	 */
	void addExpiration(final BlockHeight height, final MosaicId mosaicId, final ReadOnlyMosaicBalances balances, final ExpiredMosaicType expirationType);

	/**
	 * Removes a mosaic expiration.
	 *
	 * @param height Height of expiration.
	 * @param mosaicId Id of expiring mosaic.
	 */
	void removeExpiration(final BlockHeight height, final MosaicId mosaicId);
}
