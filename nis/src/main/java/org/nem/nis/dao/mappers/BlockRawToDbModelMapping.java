package org.nem.nis.dao.mappers;

import org.nem.core.crypto.Hash;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

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
		final DbAccount harvester = RawMapperUtils.mapAccount(this.mapper, source[6]);
		final DbAccount lessor = RawMapperUtils.mapAccount(this.mapper, source[8]);

		final DbBlock dbBlock = new DbBlock();
		dbBlock.setId(RawMapperUtils.castToLong(source[0]));
		dbBlock.setVersion((Integer) source[1]);
		dbBlock.setPrevBlockHash(new Hash((byte[]) source[2]));
		dbBlock.setBlockHash(new Hash((byte[]) source[3]));
		dbBlock.setGenerationHash(new Hash((byte[]) source[4]));
		dbBlock.setTimeStamp((Integer) source[5]);
		dbBlock.setHarvester(harvester);
		dbBlock.setHarvesterProof((byte[]) source[7]);
		dbBlock.setLessor(lessor);
		dbBlock.setHeight(RawMapperUtils.castToLong(source[9]));
		dbBlock.setTotalFee(RawMapperUtils.castToLong(source[10]));
		dbBlock.setDifficulty(RawMapperUtils.castToLong(source[11]));
		dbBlock.setBlockTransferTransactions(new ArrayList<>());
		dbBlock.setBlockImportanceTransferTransactions(new ArrayList<>());
		dbBlock.setBlockMultisigAggregateModificationTransactions(new ArrayList<>());
		dbBlock.setBlockMultisigTransactions(new ArrayList<>());

		return dbBlock;
	}
}
