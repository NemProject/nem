package org.nem.nis.cache;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.nis.dbmodel.DbMosaicId;

/**
 * A mosaic id cache.
 */
public interface MosaicIdCache extends ReadOnlyMosaicIdCache {

	/**
	 * Bidirectionally associates a mosaic id with a db mosaic id.
	 *
	 * @param mosaicId The mosaic id.
	 * @param dbMosaicId The db mosaic id.
	 */
	void add(final MosaicId mosaicId, final DbMosaicId dbMosaicId);

	/**
	 * Removes the mosaic id <--> db mosaic id mapping for the given mosaic id.
	 *
	 * @param mosaicId The mosaic id
	 */
	void remove(final MosaicId mosaicId);

	/**
	 * Removes the mosaic id <--> db mosaic id mapping for the given db mosaic id.
	 *
	 * @param dbMosaicId The db mosaic id
	 */
	void remove(final DbMosaicId dbMosaicId);

	/**
	 * Clears the cache.
	 */
	void clear();
}
