package org.nem.nis.dao.mappers;

import org.nem.core.crypto.Hash;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

/**
 * Base class for mappings of raw transfer db types to transfer db model types.
 *
 * @param <TDbModel> The db model type.
 */
public abstract class AbstractTransferRawToDbModelMapping<TDbModel extends AbstractTransfer> implements IMapping<Object[], TDbModel> {
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
		final DbAccount sender = RawMapperUtils.mapAccount(this.mapper, source[7]);

		dbModel.setId(RawMapperUtils.castToLong(source[1]));
		dbModel.setTransferHash(new Hash((byte[]) source[2]));
		dbModel.setVersion((Integer) source[3]);
		dbModel.setFee(RawMapperUtils.castToLong(source[4]));
		dbModel.setTimeStamp((Integer) source[5]);
		dbModel.setDeadline((Integer) source[6]);
		dbModel.setSender(sender);
		dbModel.setSenderProof((byte[]) source[8]);

		return dbModel;
	}

	/**
	 * Function overridden by derived classes to perform custom derived-mapping logic.
	 *
	 * @param source The source object.
	 * @return The target object.
	 */
	protected abstract TDbModel mapImpl(final Object[] source);
}
