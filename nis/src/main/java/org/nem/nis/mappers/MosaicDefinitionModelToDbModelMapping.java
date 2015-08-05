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

		// note: the mosaic id could be equal to the fee mosaic id (referencing itself as fee).
		//       in this case there is no db mosaic id available in the cache yet. We use -1 to indicate this.
		final DbMosaicId dbMosaicId = mosaicDefinition.getId().equals(mosaicDefinition.getTransferFeeInfo().getMosaicId())
				? new DbMosaicId(-1L)
				: this.mapper.map(mosaicDefinition.getTransferFeeInfo().getMosaicId(), DbMosaicId.class);
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
