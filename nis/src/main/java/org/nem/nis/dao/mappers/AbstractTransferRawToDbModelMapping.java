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
		final DbAccount sender = this.mapAccount(source[7]);

		dbModel.setId(this.castBigIntegerToLong(source[1]));
		dbModel.setTransferHash(new Hash((byte[])source[2]));
		dbModel.setVersion((Integer)source[3]);
		dbModel.setFee(this.castBigIntegerToLong(source[4]));
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

	/**
	 * Maps an account id to a db model account.
	 *
	 * @param id The account id.
	 * @return The db model account.
	 */
	protected DbAccount mapAccount(final Object id) {
		return RawMapperUtils.mapAccount(this.mapper, this.castBigIntegerToLong(id));
	}

	/**
	 * Maps a block id to a db block.
	 *
	 * @param id The block id.
	 * @return The db block.
	 */
	protected DbBlock mapBlock(final Object id) {
		final DbBlock dbBlock = new DbBlock();
		dbBlock.setId(this.castBigIntegerToLong(id));
		return dbBlock;
	}

	/**
	 * Casts a BigInteger value to a Long value.
	 *
	 * @param value The BigInteger value.
	 * @return The Long value.
	 */
	protected Long castBigIntegerToLong(final Object value) {
		return RawMapperUtils.castBigIntegerToLong((BigInteger)value);
	}
}
