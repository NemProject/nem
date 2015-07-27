package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.DbMosaicSupplyChangeTransaction;
import org.nem.nis.mappers.IMapper;

/**
 * A mapping that is able to map raw mosaic supply change transaction data to a db mosaic supply change transaction.
 */
public class MosaicSupplyChangeRawToDbModelMapping extends AbstractTransferRawToDbModelMapping<DbMosaicSupplyChangeTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicSupplyChangeRawToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public DbMosaicSupplyChangeTransaction mapImpl(final Object[] source) {
		final DbMosaicSupplyChangeTransaction dbMosaicSupplyChangeTransaction = new DbMosaicSupplyChangeTransaction();
		dbMosaicSupplyChangeTransaction.setBlock(RawMapperUtils.mapBlock(source[0]));
		dbMosaicSupplyChangeTransaction.setDbMosaicId(RawMapperUtils.castToLong(source[9]));
		dbMosaicSupplyChangeTransaction.setSupplyType((Integer)source[10]);
		dbMosaicSupplyChangeTransaction.setQuantity(RawMapperUtils.castToLong(source[11]));
		dbMosaicSupplyChangeTransaction.setBlkIndex((Integer)source[12]);
		dbMosaicSupplyChangeTransaction.setReferencedTransaction(RawMapperUtils.castToLong(source[13]));
		return dbMosaicSupplyChangeTransaction;
	}
}
