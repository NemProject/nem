package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.IMapper;

import java.math.BigInteger;

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
		final DbAccount recipient = this.mapAccount(castBigIntegerToLong((BigInteger)source[9]));

		final DbTransferTransaction dbTransfer = new DbTransferTransaction();
		dbTransfer.setBlock(mapBlock(castBigIntegerToLong((BigInteger)source[0])));
		dbTransfer.setRecipient(recipient);
		dbTransfer.setBlkIndex((Integer)source[10]);
		dbTransfer.setOrderId((Integer)source[11]);
		dbTransfer.setAmount(castBigIntegerToLong((BigInteger)source[12]));
		dbTransfer.setReferencedTransaction(castBigIntegerToLong((BigInteger)source[13]));
		dbTransfer.setMessageType((Integer)source[14]);
		dbTransfer.setMessagePayload((byte[])source[15]);

		return dbTransfer;
	}
}
