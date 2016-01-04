package org.nem.nis.mappers;

import org.nem.core.model.NemProperty;
import org.nem.nis.dbmodel.DbMosaicProperty;

/**
 * A mapping that is able to map a db mosaic property to a model nem property.
 */
public class MosaicPropertyDbModelToModelMapping implements IMapping<DbMosaicProperty, NemProperty> {

	@Override
	public NemProperty map(final DbMosaicProperty dbMosaicProperty) {
		return new NemProperty(dbMosaicProperty.getName(), dbMosaicProperty.getValue());
	}
}
