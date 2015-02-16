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
		final DbAccount harvester = RawMapperUtils.mapAccount(this.mapper, (BigInteger)source[7]);
		final DbAccount lessor = RawMapperUtils.mapAccount(this.mapper, (BigInteger)source[9]);

		final DbBlock dbBlock = new DbBlock();
		dbBlock.setId(RawMapperUtils.castBigIntegerToLong((BigInteger)source[0]));
		dbBlock.setShortId(RawMapperUtils.castBigIntegerToLong((BigInteger)source[1]));
		dbBlock.setVersion((Integer)source[2]);
		dbBlock.setPrevBlockHash(new Hash((byte[])source[3]));
		dbBlock.setBlockHash(new Hash((byte[])source[4]));
		dbBlock.setGenerationHash(new Hash((byte[])source[5]));
		dbBlock.setTimeStamp((Integer)source[6]);
		dbBlock.setHarvester(harvester);
		dbBlock.setHarvesterProof((byte[])source[8]);
		dbBlock.setLessor(lessor);
		dbBlock.setHeight(RawMapperUtils.castBigIntegerToLong((BigInteger)source[10]));
		dbBlock.setTotalFee(RawMapperUtils.castBigIntegerToLong((BigInteger)source[11]));
		dbBlock.setDifficulty(RawMapperUtils.castBigIntegerToLong((BigInteger)source[12]));
		dbBlock.setBlockTransferTransactions(new ArrayList<>());
		dbBlock.setBlockImportanceTransferTransactions(new ArrayList<>());
		dbBlock.setBlockMultisigAggregateModificationTransactions(new ArrayList<>());
		dbBlock.setBlockMultisigTransactions(new ArrayList<>());

		return dbBlock;
	}
}
