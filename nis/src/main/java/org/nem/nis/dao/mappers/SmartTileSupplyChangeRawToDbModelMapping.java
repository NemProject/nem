package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.DbSmartTileSupplyChangeTransaction;
import org.nem.nis.mappers.IMapper;

/**
 * A mapping that is able to map raw smart tile supply change transaction data to a db smart tile supply change transaction.
 */
public class SmartTileSupplyChangeRawToDbModelMapping extends AbstractTransferRawToDbModelMapping<DbSmartTileSupplyChangeTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public SmartTileSupplyChangeRawToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public DbSmartTileSupplyChangeTransaction mapImpl(final Object[] source) {
		final DbSmartTileSupplyChangeTransaction dbSmartTileSupplyChangeTransaction = new DbSmartTileSupplyChangeTransaction();
		dbSmartTileSupplyChangeTransaction.setBlock(RawMapperUtils.mapBlock(source[0]));
		dbSmartTileSupplyChangeTransaction.setDbMosaicId(RawMapperUtils.castToLong(source[9]));
		dbSmartTileSupplyChangeTransaction.setSupplyType((Integer)source[10]);
		dbSmartTileSupplyChangeTransaction.setQuantity(RawMapperUtils.castToLong(source[11]));
		dbSmartTileSupplyChangeTransaction.setBlkIndex((Integer)source[12]);
		dbSmartTileSupplyChangeTransaction.setReferencedTransaction(RawMapperUtils.castToLong(source[13]));
		return dbSmartTileSupplyChangeTransaction;
	}
}
