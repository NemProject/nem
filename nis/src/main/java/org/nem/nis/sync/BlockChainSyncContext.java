package org.nem.nis.sync;

import org.nem.core.model.primitive.*;
import org.nem.nis.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.service.BlockChainLastBlockLayer;

/**
 * Creates a block chain synchronization context. The primary point of this class is to
 * hold onto a copy of the account analyzer so that all account-related modifications during
 * a sync only modify the copy.
 */
public class BlockChainSyncContext {
	private final AccountAnalyzer accountAnalyzer;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockDao blockDao;
	private final BlockChainServices services;
	private final BlockChainScore ourScore;

	public BlockChainSyncContext(
			final AccountAnalyzer accountAnalyzer,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockDao blockDao,
			final BlockChainServices services,
			final BlockChainScore ourScore) {
		this.accountAnalyzer = accountAnalyzer.copy();
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockDao = blockDao;
		this.services = services;
		this.ourScore = ourScore;
	}

	/**
	 * Gets the working copy of the account analyzer.
	 *
	 * @return The account analyzer.
	 */
	public AccountAnalyzer accountAnalyzer() {
		return this.accountAnalyzer;
	}

	/**
	 * Reverses transactions between commonBlockHeight and current lastBlock.
	 * Additionally calculates score.
	 *
	 * @param commonBlockHeight height up to which TXes should be reversed.
	 * @return score for iterated blocks.
	 */
	public BlockChainScore undoTxesAndGetScore(final BlockHeight commonBlockHeight) {
		return this.services.undoAndGetScore(this.accountAnalyzer, this.createLocalBlockLookup(), commonBlockHeight);
	}

	/**
	 * Creates a local block lookup adapter.
	 *
	 * @return The local block lookup adapter.
	 */
	public BlockLookup createLocalBlockLookup() {
		return new LocalBlockLookupAdapter(
				this.blockDao,
				this.accountAnalyzer.getAccountCache(),
				this.blockChainLastBlockLayer.getLastDbBlock(),
				this.ourScore,
				BlockChainConstants.BLOCKS_LIMIT);
	}
}