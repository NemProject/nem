package org.nem.nis.sync;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.NisDbModelToModelMapper;

/**
 * A BlockLookup implementation that looks up blocks from a local node.
 */
public class LocalBlockLookupAdapter implements BlockLookup {
	private final ReadOnlyBlockDao blockDao;
	private final NisDbModelToModelMapper mapper;
	private final Block lastBlock;
	private final BlockChainScore chainScore;
	private final int maxHashesToReturn;

	/**
	 * Creates a new local block lookup adapter.
	 *
	 * @param blockDao The block database.
	 * @param mapper The mapper.
	 * @param lastBlock The last block.
	 * @param chainScore The chain score.
	 * @param maxHashesToReturn The maximum number of hashes to return.
	 */
	public LocalBlockLookupAdapter(final ReadOnlyBlockDao blockDao, final NisDbModelToModelMapper mapper, final DbBlock lastBlock,
			final BlockChainScore chainScore, final int maxHashesToReturn) {
		this.blockDao = blockDao;
		this.mapper = mapper;
		this.lastBlock = this.mapper.map(lastBlock);
		this.chainScore = chainScore;
		this.maxHashesToReturn = maxHashesToReturn;
	}

	@Override
	public Block getLastBlock() {
		return this.lastBlock;
	}

	@Override
	public BlockChainScore getChainScore() {
		return this.chainScore;
	}

	@Override
	public Block getBlockAt(final BlockHeight height) {
		final DbBlock dbBlock = this.blockDao.findByHeight(height);
		return this.mapper.map(dbBlock);
	}

	@Override
	public HashChain getHashesFrom(final BlockHeight height) {
		return this.blockDao.getHashesFrom(height, this.maxHashesToReturn);
	}
}
