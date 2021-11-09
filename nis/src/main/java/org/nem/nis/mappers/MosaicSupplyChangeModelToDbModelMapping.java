package org.nem.nis.mappers;

import org.nem.core.model.MosaicSupplyChangeTransaction;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a model mosaic supply change transaction to a db mosaic supply change transaction.
 */
public class MosaicSupplyChangeModelToDbModelMapping
		extends
			AbstractTransferModelToDbModelMapping<MosaicSupplyChangeTransaction, DbMosaicSupplyChangeTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicSupplyChangeModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	protected DbMosaicSupplyChangeTransaction mapImpl(final MosaicSupplyChangeTransaction source) {
		final DbMosaicId dbMosaicId = this.mapper.map(source.getMosaicId(), DbMosaicId.class);
		final DbMosaicSupplyChangeTransaction dbTransaction = new DbMosaicSupplyChangeTransaction();
		dbTransaction.setDbMosaicId(dbMosaicId.getId());
		dbTransaction.setSupplyType(source.getSupplyType().value());
		dbTransaction.setQuantity(source.getDelta().getRaw());
		dbTransaction.setReferencedTransaction(0L);
		return dbTransaction;
	}
}
