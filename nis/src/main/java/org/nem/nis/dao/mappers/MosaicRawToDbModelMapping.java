package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.DbMosaic;
import org.nem.nis.mappers.IMapping;

/**
 * A mapping that is able to map raw mosaic data to a db mosaic.
 */
public class MosaicRawToDbModelMapping implements IMapping<Object[], DbMosaic> {
	@Override
	public DbMosaic map(final Object[] source) {
		final DbMosaic dbMosaic = new DbMosaic();
		dbMosaic.setId(RawMapperUtils.castToLong(source[0]));
		dbMosaic.setDbMosaicId(RawMapperUtils.castToLong(source[1]));
		dbMosaic.setQuantity(RawMapperUtils.castToLong(source[2]));
		return dbMosaic;
	}
}
