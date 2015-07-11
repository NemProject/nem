package org.nem.nis.mappers;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.nis.cache.ReadOnlyMosaicIdCache;
import org.nem.nis.dbmodel.DbMosaicId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A mapping that is able to map a model mosaic id to a db model mosaic id.
 */
public class MosaicIdModelToDbModelMapping implements IMapping<MosaicId, DbMosaicId> {
	private final ReadOnlyMosaicIdCache mosaicIdCache;

	/**
	 * Creates a new mapping.
	 *
	 * @param mosaicIdCache The mosaic id cache.
	 */
	@Autowired(required = true)
	public MosaicIdModelToDbModelMapping(final ReadOnlyMosaicIdCache mosaicIdCache) {
		this.mosaicIdCache = mosaicIdCache;
	}

	@Override
	public DbMosaicId map(final MosaicId mosaicId) {
		return this.mosaicIdCache.get(mosaicId);
	}
}
