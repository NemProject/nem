package org.nem.nis.mappers;

import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.nis.dbmodel.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a model mosaic definition to a db mosaic definition.
 */
public class MosaicDefinitionModelToDbModelMapping implements IMapping<MosaicDefinition, DbMosaicDefinition> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicDefinitionModelToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public DbMosaicDefinition map(final MosaicDefinition mosaicDefinition) {
		final Set<DbMosaicProperty> mosaicProperties = mosaicDefinition.getProperties().asCollection().stream()
				.map(p -> this.mapper.map(p, DbMosaicProperty.class))
				.collect(Collectors.toSet());
		final DbMosaicDefinition dbMosaicDefinition = new DbMosaicDefinition();
		mosaicProperties.forEach(p -> p.setMosaicDefinition(dbMosaicDefinition));
		dbMosaicDefinition.setCreator(this.mapper.map(mosaicDefinition.getCreator(), DbAccount.class));
		dbMosaicDefinition.setName(mosaicDefinition.getId().getName());
		dbMosaicDefinition.setDescription(mosaicDefinition.getDescriptor().toString());
		dbMosaicDefinition.setNamespaceId(mosaicDefinition.getId().getNamespaceId().toString());
		dbMosaicDefinition.setProperties(mosaicProperties);
		return dbMosaicDefinition;
	}
}
