package org.nem.nis.mappers;

import org.nem.core.model.MosaicCreationTransaction;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a model mosaic creation transaction to a db mosaic creation transaction.
 */
public class MosaicCreationModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<MosaicCreationTransaction, DbMosaicCreationTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicCreationModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	protected DbMosaicCreationTransaction mapImpl(final MosaicCreationTransaction source) {
		final DbMosaic dbMosaic = this.mapper.map(source.getMosaic(), DbMosaic.class);

		final DbMosaicCreationTransaction dbTransaction = new DbMosaicCreationTransaction();
		dbTransaction.setMosaic(dbMosaic);
		dbTransaction.setReferencedTransaction(0L);
		return dbTransaction;
	}
}
