package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.dbmodel.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A mapping that is able to map a db mosaic definition to a model mosaic definition.
 */
public class MosaicDefinitionDbModelToModelMapping implements IMapping<DbMosaicDefinition, MosaicDefinition> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicDefinitionDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public MosaicDefinition map(final DbMosaicDefinition dbMosaicDefinition) {
		final Account creator = this.mapper.map(dbMosaicDefinition.getCreator(), Account.class);
		final Account feeRecipient = this.mapper.map(dbMosaicDefinition.getFeeRecipient(), Account.class);
		final MosaicId mosaicId = new MosaicId(new NamespaceId(dbMosaicDefinition.getNamespaceId()), dbMosaicDefinition.getName());

		// note: a value of -1 for the db fee mosaic id means the fee info should have the same mosaic id as the mosaic definition.
		final MosaicId feeMosaicId = dbMosaicDefinition.getFeeDbMosaicId().equals(-1L)
				? mosaicId
				: this.mapper.map(new DbMosaicId(dbMosaicDefinition.getFeeDbMosaicId()), MosaicId.class);
		final List<NemProperty> properties = dbMosaicDefinition.getProperties().stream()
				.map(p -> this.mapper.map(p, NemProperty.class))
				.collect(Collectors.toList());
		final MosaicLevy feeInfo = new MosaicLevy(
				MosaicTransferFeeType.fromValue(dbMosaicDefinition.getFeeType()),
				feeRecipient,
				feeMosaicId,
				Quantity.fromValue(dbMosaicDefinition.getFeeQuantity()));
		return new MosaicDefinition(
				creator,
				mosaicId,
				new MosaicDescriptor(dbMosaicDefinition.getDescription()),
				new DefaultMosaicProperties(properties),
				feeInfo);
	}
}
