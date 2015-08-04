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

		// TODO 20150804 BR -> BR: beware of self referencing mosaic definitions!
		final DbMosaicId dbMosaicId = this.mapper.map(mosaicDefinition.getTransferFeeInfo().getMosaicId(), DbMosaicId.class);
		final DbMosaicDefinition dbMosaicDefinition = new DbMosaicDefinition();
		mosaicProperties.forEach(p -> p.setMosaicDefinition(dbMosaicDefinition));
		dbMosaicDefinition.setCreator(this.mapper.map(mosaicDefinition.getCreator(), DbAccount.class));
		dbMosaicDefinition.setName(mosaicDefinition.getId().getName());
		dbMosaicDefinition.setDescription(mosaicDefinition.getDescriptor().toString());
		dbMosaicDefinition.setNamespaceId(mosaicDefinition.getId().getNamespaceId().toString());
		dbMosaicDefinition.setProperties(mosaicProperties);
		dbMosaicDefinition.setFeeType(mosaicDefinition.getTransferFeeInfo().getType().value());
		dbMosaicDefinition.setFeeRecipient(this.mapper.map(mosaicDefinition.getTransferFeeInfo().getRecipient(), DbAccount.class));
		dbMosaicDefinition.setFeeDbMosaicId(dbMosaicId.getId());
		dbMosaicDefinition.setFeeQuantity(mosaicDefinition.getTransferFeeInfo().getFee().getRaw());
		return dbMosaicDefinition;
	}
}
