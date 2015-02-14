package org.nem.nis.dao.mappers;

import org.nem.core.crypto.Hash;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * A mapping that is able to map raw block data to an empty db block.
 */
public class BlockRawToDbModelMapping implements IMapping<Object[], DbBlock> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public BlockRawToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public DbBlock map(final Object[] source) {
		final DbAccount harvester = this.mapAccount(source[7]);
		final DbAccount lessor = this.mapAccount(source[9]);

		final DbBlock dbBlock = new DbBlock();
		dbBlock.setId(castBigIntegerToLong(source[0]));
		dbBlock.setShortId(castBigIntegerToLong(source[1]));
		dbBlock.setVersion((Integer)source[2]);
		dbBlock.setPrevBlockHash(new Hash((byte[])source[3]));
		dbBlock.setBlockHash(new Hash((byte[])source[4]));
		dbBlock.setGenerationHash(new Hash((byte[])source[5]));
		dbBlock.setTimeStamp((Integer)source[6]);
		dbBlock.setHarvester(harvester);
		dbBlock.setHarvesterProof((byte[])source[8]);
		dbBlock.setLessor(lessor);
		dbBlock.setHeight(castBigIntegerToLong(source[10]));
		dbBlock.setTotalFee(castBigIntegerToLong(source[11]));
		dbBlock.setDifficulty(castBigIntegerToLong(source[12]));
		dbBlock.setBlockTransferTransactions(new ArrayList<>());
		dbBlock.setBlockImportanceTransferTransactions(new ArrayList<>());
		dbBlock.setBlockMultisigAggregateModificationTransactions(new ArrayList<>());
		dbBlock.setBlockMultisigTransactions(new ArrayList<>());

		return dbBlock;
	}

	private DbAccount mapAccount(final Object id) {
		return RawMapperUtils.mapAccount(this.mapper, castBigIntegerToLong(id));
	}

	private static Long castBigIntegerToLong(final Object value) {
		return RawMapperUtils.castBigIntegerToLong((BigInteger)value);
	}
}
