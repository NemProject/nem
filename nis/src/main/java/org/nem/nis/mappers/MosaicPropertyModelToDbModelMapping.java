package org.nem.nis.mappers;

import org.nem.core.model.NemProperty;
import org.nem.nis.dbmodel.DbMosaicProperty;

/**
 * A mapping that is able to map a model nem property to a db mosaic property.
 */
public class MosaicPropertyModelToDbModelMapping implements IMapping<NemProperty, DbMosaicProperty> {

	@Override
	public DbMosaicProperty map(final NemProperty property) {
		final DbMosaicProperty dbMosaicProperty = new DbMosaicProperty();
		dbMosaicProperty.setName(property.getName());
		dbMosaicProperty.setValue(property.getValue());
		return dbMosaicProperty;
	}
}
