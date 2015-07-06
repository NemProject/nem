package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.IMapper;

import java.util.Arrays;

/**
 * A mapping that is able to map raw mosaic creation transaction data to a db mosaic creation transaction.
 */
public class MosaicCreationRawToDbModelMapping extends AbstractTransferRawToDbModelMapping<DbMosaicCreationTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicCreationRawToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	public DbMosaicCreationTransaction mapImpl(final Object[] source) {
		final DbMosaic dbMosaic = this.mapper.map(Arrays.copyOfRange(source, 12, source.length), DbMosaic.class);
		final DbMosaicCreationTransaction dbMosaicCreationTransaction = new DbMosaicCreationTransaction();
		dbMosaicCreationTransaction.setBlock(RawMapperUtils.mapBlock(source[0]));
		dbMosaicCreationTransaction.setBlkIndex((Integer)source[10]);
		dbMosaicCreationTransaction.setReferencedTransaction(RawMapperUtils.castToLong(source[11]));
		dbMosaicCreationTransaction.setMosaic(dbMosaic);
		return dbMosaicCreationTransaction;
	}
}
