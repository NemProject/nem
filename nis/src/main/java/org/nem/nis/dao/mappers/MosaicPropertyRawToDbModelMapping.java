package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

/**
 * A mapping that is able to map raw mosaic property data to a db mosaic property.
 */
public class MosaicPropertyRawToDbModelMapping implements IMapping<Object[], DbMosaicProperty> {
	@Override
	public DbMosaicProperty map(final Object[] source) {
		final DbMosaicProperty dbMosaicProperty = new DbMosaicProperty();
		dbMosaicProperty.setId(RawMapperUtils.castToLong(source[8]));
		dbMosaicProperty.setName((String)source[9]);
		dbMosaicProperty.setValue((String)source[10]);
		return dbMosaicProperty;
	}
}
