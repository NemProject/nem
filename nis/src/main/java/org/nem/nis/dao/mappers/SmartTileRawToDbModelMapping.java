package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.DbSmartTile;
import org.nem.nis.mappers.IMapping;

/**
 * A mapping that is able to map raw smart tile data to a db smart tile.
 */
public class SmartTileRawToDbModelMapping implements IMapping<Object[], DbSmartTile> {
	@Override
	public DbSmartTile map(final Object[] source) {
		final DbSmartTile dbSmartTile = new DbSmartTile();
		dbSmartTile.setId(RawMapperUtils.castToLong(source[0]));
		dbSmartTile.setDbMosaicId(RawMapperUtils.castToLong(source[1]));
		dbSmartTile.setQuantity(RawMapperUtils.castToLong(source[2]));
		return dbSmartTile;
	}
}
