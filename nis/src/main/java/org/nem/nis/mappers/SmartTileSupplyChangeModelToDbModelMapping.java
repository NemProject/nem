package org.nem.nis.mappers;

import org.nem.core.model.SmartTileSupplyChangeTransaction;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a model smart tile supply change transaction to a db smart tile supply change transaction.
 */
public class SmartTileSupplyChangeModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<SmartTileSupplyChangeTransaction, DbSmartTileSupplyChangeTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public SmartTileSupplyChangeModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	protected DbSmartTileSupplyChangeTransaction mapImpl(final SmartTileSupplyChangeTransaction source) {
		final DbMosaicId dbMosaicId = this.mapper.map(source.getMosaicId(), DbMosaicId.class);
		final DbSmartTileSupplyChangeTransaction dbTransaction = new DbSmartTileSupplyChangeTransaction();
		dbTransaction.setDbMosaicId(dbMosaicId.getId());
		dbTransaction.setSupplyType(source.getSupplyType().value());
		dbTransaction.setQuantity(source.getDelta().getRaw());
		dbTransaction.setReferencedTransaction(0L);
		return dbTransaction;
	}
}
