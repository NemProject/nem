package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.DbMosaicDefinitionCreationTransaction;

/**
 * A mapping that is able to map a db mosaic definition creation transaction to a model mosaic definition creation transaction.
 */
public class MosaicDefinitionCreationDbModelToModelMapping
		extends
			AbstractTransferDbModelToModelMapping<DbMosaicDefinitionCreationTransaction, MosaicDefinitionCreationTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicDefinitionCreationDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	protected MosaicDefinitionCreationTransaction mapImpl(final DbMosaicDefinitionCreationTransaction source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);
		final Account creationFeeSink = this.mapper.map(source.getCreationFeeSink(), Account.class);

		final MosaicDefinition mosaicDefinition = this.mapper.map(source.getMosaicDefinition(), MosaicDefinition.class);
		return new MosaicDefinitionCreationTransaction(new TimeInstant(source.getTimeStamp()), sender, mosaicDefinition, creationFeeSink,
				Amount.fromMicroNem(source.getCreationFee()));
	}
}
