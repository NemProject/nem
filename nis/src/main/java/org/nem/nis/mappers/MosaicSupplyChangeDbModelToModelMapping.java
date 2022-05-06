package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.Supply;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a db mosaic supply change transaction to a model mosaic supply change transaction.
 */
public class MosaicSupplyChangeDbModelToModelMapping
		extends
			AbstractTransferDbModelToModelMapping<DbMosaicSupplyChangeTransaction, MosaicSupplyChangeTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicSupplyChangeDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	protected MosaicSupplyChangeTransaction mapImpl(final DbMosaicSupplyChangeTransaction source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);
		final MosaicId mosaicId = this.mapper.map(new DbMosaicId(source.getDbMosaicId()), MosaicId.class);

		return new MosaicSupplyChangeTransaction(new TimeInstant(source.getTimeStamp()), sender, mosaicId,
				MosaicSupplyType.fromValueOrDefault(source.getSupplyType()), Supply.fromValue(source.getQuantity()));
	}
}
