package org.nem.nis.mappers;

import org.nem.core.model.mosaic.*;
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
				.map(p -> this.mapper.map(p, DbMosaicProperty.class)).collect(Collectors.toSet());

		final DbMosaicDefinition dbMosaicDefinition = new DbMosaicDefinition();
		mosaicProperties.forEach(p -> p.setMosaicDefinition(dbMosaicDefinition));
		dbMosaicDefinition.setCreator(this.mapper.map(mosaicDefinition.getCreator(), DbAccount.class));
		dbMosaicDefinition.setName(mosaicDefinition.getId().getName());
		dbMosaicDefinition.setDescription(mosaicDefinition.getDescriptor().toString());
		dbMosaicDefinition.setNamespaceId(mosaicDefinition.getId().getNamespaceId().toString());
		dbMosaicDefinition.setProperties(mosaicProperties);

		if (mosaicDefinition.isMosaicLevyPresent()) {
			final MosaicLevy levy = mosaicDefinition.getMosaicLevy();

			// note: the mosaic id could be equal to the fee mosaic id (referencing itself as fee).
			// in this case there is no db mosaic id available in the cache yet. We use -1 to indicate this.
			final DbMosaicId dbMosaicId = mosaicDefinition.getId().equals(levy.getMosaicId())
					? new DbMosaicId(-1L)
					: this.mapper.map(levy.getMosaicId(), DbMosaicId.class);

			dbMosaicDefinition.setFeeType(levy.getType().value());
			dbMosaicDefinition.setFeeRecipient(this.mapper.map(levy.getRecipient(), DbAccount.class));
			dbMosaicDefinition.setFeeDbMosaicId(dbMosaicId.getId());
			dbMosaicDefinition.setFeeQuantity(levy.getFee().getRaw());
		}

		return dbMosaicDefinition;
	}
}
