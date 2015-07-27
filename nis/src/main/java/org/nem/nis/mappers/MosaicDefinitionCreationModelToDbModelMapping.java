package org.nem.nis.mappers;

import org.nem.core.model.MosaicDefinitionCreationTransaction;
import org.nem.nis.dbmodel.*;

/**
 * A mapping that is able to map a model mosaic definition creation transaction to a db mosaic definition creation transaction.
 */
public class MosaicDefinitionCreationModelToDbModelMapping extends AbstractTransferModelToDbModelMapping<MosaicDefinitionCreationTransaction, DbMosaicDefinitionCreationTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicDefinitionCreationModelToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	protected DbMosaicDefinitionCreationTransaction mapImpl(final MosaicDefinitionCreationTransaction source) {
		final DbMosaicDefinition dbMosaicDefinition = this.mapper.map(source.getMosaicDefinition(), DbMosaicDefinition.class);

		final DbMosaicDefinitionCreationTransaction dbTransaction = new DbMosaicDefinitionCreationTransaction();
		dbTransaction.setMosaicDefinition(dbMosaicDefinition);
		dbTransaction.setReferencedTransaction(0L);
		return dbTransaction;
	}
}
