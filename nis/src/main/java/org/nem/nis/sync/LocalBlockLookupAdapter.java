package org.nem.nis.sync;

import org.nem.core.model.Block;
import org.nem.core.model.HashChain;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.mappers.BlockMapper;

/**
 * A BlockLookup implementation that looks up blocks from a local node.
 */
public class LocalBlockLookupAdapter implements BlockLookup {

	private final BlockDao blockDao;
	private final AccountLookup accountLookup;
	private final Block lastBlock;
	private final int maxHashesToReturn;

	/**
	 * Creates a new local block lookup adapter.
	 *
	 * @param blockDao The block database.
	 * @param accountLookup The account lookup to use.
	 * @param lastBlock The last block.
	 * @param maxHashesToReturn The maximum number of hashes to return.
	 */
	public LocalBlockLookupAdapter(
			final BlockDao blockDao,
			final AccountLookup accountLookup,
			final org.nem.nis.dbmodel.Block lastBlock,
			final int maxHashesToReturn) {
		this.blockDao = blockDao;
		this.accountLookup = accountLookup;
		this.lastBlock = BlockMapper.toModel(lastBlock, this.accountLookup);
		this.maxHashesToReturn = maxHashesToReturn;
	}

	@Override
	public Block getLastBlock() {
		return this.lastBlock;
	}

	@Override
	public Block getBlockAt(long height) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(height);
		return BlockMapper.toModel(dbBlock, this.accountLookup);
	}

	@Override
	public HashChain getHashesFrom(long height) {
		return new HashChain(blockDao.getHashesFrom(height, this.maxHashesToReturn));
	}
}