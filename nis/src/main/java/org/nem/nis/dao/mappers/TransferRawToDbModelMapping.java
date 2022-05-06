package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.IMapper;

/**
 * A mapping that is able to map raw transfer transaction data to a db transfer.
 */
public class TransferRawToDbModelMapping extends AbstractTransferRawToDbModelMapping<DbTransferTransaction> {

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public TransferRawToDbModelMapping(final IMapper mapper) {
		super(mapper);
	}

	@Override
	protected DbTransferTransaction mapImpl(final Object[] source) {
		final DbAccount recipient = RawMapperUtils.mapAccount(this.mapper, source[9]);

		final DbTransferTransaction dbTransfer = new DbTransferTransaction();
		dbTransfer.setBlock(RawMapperUtils.mapBlock(source[0]));
		dbTransfer.setRecipient(recipient);
		dbTransfer.setBlkIndex((Integer) source[10]);
		dbTransfer.setAmount(RawMapperUtils.castToLong(source[11]));
		dbTransfer.setReferencedTransaction(RawMapperUtils.castToLong(source[12]));
		dbTransfer.setMessageType((Integer) source[13]);
		dbTransfer.setMessagePayload((byte[]) source[14]);

		return dbTransfer;
	}
}
