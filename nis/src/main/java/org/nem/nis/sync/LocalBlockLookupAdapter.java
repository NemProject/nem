package org.nem.nis.sync;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.mappers.BlockMapper;

/**
 * A BlockLookup implementation that looks up blocks from a local node.
 */
public class LocalBlockLookupAdapter implements BlockLookup {

	private final ReadOnlyBlockDao blockDao;
	private final AccountLookup accountLookup;
	private final Block lastBlock;
	private final BlockChainScore chainScore;
	private final int maxHashesToReturn;

	/**
	 * Creates a new local block lookup adapter.
	 *
	 * @param blockDao The block database.
	 * @param accountLookup The account lookup to use.
	 * @param lastBlock The last block.
	 * @param chainScore The chain score.
	 * @param maxHashesToReturn The maximum number of hashes to return.
	 */
	public LocalBlockLookupAdapter(
			final ReadOnlyBlockDao blockDao,
			final AccountLookup accountLookup,
			final org.nem.nis.dbmodel.Block lastBlock,
			final BlockChainScore chainScore,
			final int maxHashesToReturn) {
		this.blockDao = blockDao;
		this.accountLookup = accountLookup;
		this.lastBlock = BlockMapper.toModel(lastBlock, this.accountLookup);
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
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(height);
		return BlockMapper.toModel(dbBlock, this.accountLookup);
	}

	@Override
	public HashChain getHashesFrom(final BlockHeight height) {
		return blockDao.getHashesFrom(height, this.maxHashesToReturn);
	}
}
