package org.nem.nis.dao.mappers;

import org.nem.core.crypto.Hash;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

/**
 * Base class for mappings of raw transfer db types to transfer db model types.
 *
 * @param <TDbModel> The db model type.
 */
public abstract class AbstractTransferRawToDbModelMapping<TDbModel extends AbstractTransfer>
		implements IMapping<Object[], TDbModel> {
	protected final IMapper mapper;

	/**
	 * Creates a mapping.
	 *
	 * @param mapper The mapper.
	 */
	protected AbstractTransferRawToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public TDbModel map(final Object[] source) {
		final TDbModel dbModel = this.mapImpl(source);
		final DbAccount sender = RawMapperUtils.mapAccount(this.mapper, (BigInteger)source[7]);

		dbModel.setId(RawMapperUtils.castBigIntegerToLong((BigInteger)source[1]));
		dbModel.setTransferHash(new Hash((byte[])source[2]));
		dbModel.setVersion((Integer)source[3]);
		dbModel.setFee(RawMapperUtils.castBigIntegerToLong((BigInteger)source[4]));
		dbModel.setTimeStamp((Integer)source[5]);
		dbModel.setDeadline((Integer)source[6]);
		dbModel.setSender(sender);
		dbModel.setSenderProof((byte[])source[8]);

		return dbModel;
	}

	/**
	 * Function overridden by derived classes to perform custom derived-mapping logic.
	 *
	 * @param source The source object.
	 * @return The target object.
	 */
	protected abstract TDbModel mapImpl(final Object[] source);

	// TODO 20150213 J-B: since we have up two three occurrences of these functions, we might want to move them all to RawMapperUtils
	// TODO 20150216 BR -> ok.
}
