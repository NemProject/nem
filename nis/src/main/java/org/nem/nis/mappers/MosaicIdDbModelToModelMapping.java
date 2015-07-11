package org.nem.nis.mappers;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.nis.cache.ReadOnlyMosaicIdCache;
import org.nem.nis.dbmodel.DbMosaicId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A mapping that is able to map a db model mosaic id to a model mosaic id.
 */
public class MosaicIdDbModelToModelMapping implements IMapping<DbMosaicId, MosaicId> {
	private final ReadOnlyMosaicIdCache mosaicIdCache;

	/**
	 * Creates a new mapping.
	 *
	 * @param mosaicIdCache The mosaic id cache.
	 */
	@Autowired(required = true)
	public MosaicIdDbModelToModelMapping(final ReadOnlyMosaicIdCache mosaicIdCache) {
		this.mosaicIdCache = mosaicIdCache;
	}

	@Override
	public MosaicId map(final DbMosaicId dbMosaicId) {
		return this.mosaicIdCache.get(dbMosaicId);
	}
}
