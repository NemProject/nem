package org.nem.nis.sync;

import org.nem.core.model.BlockChainConstants;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.service.BlockChainLastBlockLayer;

/**
 * Creates a block chain synchronization context. The primary point of this class is to
 * hold onto a copy of the account analyzer so that all account-related modifications during
 * a sync only modify the copy.
 */
public class BlockChainSyncContext {
	private final NisCache nisCache;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockDao blockDao;
	private final BlockChainServices services;
	private final BlockChainScore ourScore;

	public BlockChainSyncContext(
			final ReadOnlyNisCache nisCache,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockDao blockDao,
			final BlockChainServices services,
			final BlockChainScore ourScore) {
		this.nisCache = nisCache.copy();
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockDao = blockDao;
		this.services = services;
		this.ourScore = ourScore;
	}

	/**
	 * Gets the working copy of the NIS cache.
	 *
	 * @return The NIS cache.
	 */
	public NisCache nisCache() {
		return this.nisCache;
	}

	/**
	 * Reverses transactions between commonBlockHeight and current lastBlock.
	 * Additionally calculates score.
	 *
	 * @param commonBlockHeight height up to which TXes should be reversed.
	 * @return score for iterated blocks.
	 */
	public BlockChainScore undoTxesAndGetScore(final BlockHeight commonBlockHeight) {
		return this.services.undoAndGetScore(this.nisCache, this.createLocalBlockLookup(), commonBlockHeight);
	}

	/**
	 * Creates a local block lookup adapter.
	 *
	 * @return The local block lookup adapter.
	 */
	public BlockLookup createLocalBlockLookup() {
		return new LocalBlockLookupAdapter(
				this.blockDao,
				this.services.createMapper(this.nisCache.getAccountCache()),
				this.blockChainLastBlockLayer.getLastDbBlock(),
				this.ourScore,
				BlockChainConstants.BLOCKS_LIMIT);
	}
}