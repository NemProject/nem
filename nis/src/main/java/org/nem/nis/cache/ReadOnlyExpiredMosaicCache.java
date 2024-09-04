package org.nem.nis.cache;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.*;

import java.util.*;

public interface ReadOnlyExpiredMosaicCache {
	/**
	 * Gets the number of heights with mosaic expirations.
	 *
	 * @return Number of heights with mosaic expirations.
	 */
	int size();

	/**
	 * Gets the number of mosaic expirations across all heights.
	 *
	 * @return Number of mosaic expirations across all heights.
	 */
	int deepSize();

	/**
	 * Finds all mosaic expirations at specified height.
	 *
	 * @param height Height of expiration.
	 * @return All expiring mosaics at height.
	 */
	Collection<ExpiredMosaicEntry> findExpirationsAtHeight(BlockHeight height);
}
