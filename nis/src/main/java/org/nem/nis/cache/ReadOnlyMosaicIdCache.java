package org.nem.nis.cache;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.nis.dbmodel.DbMosaicId;

/**
 * A readonly mosaic id cache.
 */
public interface ReadOnlyMosaicIdCache {

	/**
	 * Gets the number of mappings.
	 *
	 * @return The number of mappings.
	 */
	int size();

	/**
	 * Gets the db mosaic id from a mosaic id.
	 * TODO 20150714 J-B: isn't DbMosaicId mutable? do you really need a read-only cache?
	 *
	 * @param mosaicId The mosaic id.
	 * @return The db mosaic id.
	 */
	DbMosaicId get(final MosaicId mosaicId);

	/**
	 * Gets the mosaic id from a db mosaic id.
	 *
	 * @param dbMosaicId The db mosaic id.
	 * @return The mosaic id.
	 */
	MosaicId get(final DbMosaicId dbMosaicId);

	/**
	 * Returns a value indicating whether or not the cache contains a mosaic id <--> db mosaic id mapping for a given mosaic id.
	 *
	 * @param mosaicId The mosaic id.
	 * @return true if a mosaic id <--> db mosaic id mapping exists in the cache, false otherwise.
	 */
	boolean contains(final MosaicId mosaicId);

	/**
	 * Returns a value indicating whether or not the cache contains a mosaic id <--> db mosaic id mapping for a given db mosaic id.
	 *
	 * @param dbMosaicId The db mosaic id.
	 * @return true if a mosaic id <--> db mosaic id mapping exists in the cache, false otherwise.
	 */
	boolean contains(final DbMosaicId dbMosaicId);
}
