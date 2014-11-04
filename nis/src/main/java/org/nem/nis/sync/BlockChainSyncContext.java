package org.nem.nis.sync;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.nis.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.service.BlockChainLastBlockLayer;

import java.util.Collection;

public class BlockChainSyncContext {
	private final AccountAnalyzer accountAnalyzer;
	private final AccountAnalyzer originalAnalyzer;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockDao blockDao;
	private final BlockChainServices services;
	private final BlockChainScore ourScore;

	public BlockChainSyncContext(
			final AccountAnalyzer accountAnalyzer,
			final AccountAnalyzer originalAnalyzer,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockDao blockDao,
			final BlockChainServices services,
			final BlockChainScore ourScore) {
		this.accountAnalyzer = accountAnalyzer;
		this.originalAnalyzer = originalAnalyzer;
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

	public UpdateChainResult updateOurChain(
			final UnconfirmedTransactions unconfirmedTransactions,
			final org.nem.nis.dbmodel.Block dbParentBlock,
			final Collection<Block> peerChain,
			final BlockChainScore ourScore,
			final boolean hasOwnChain) {

		final BlockChainUpdateContext updateContext = new BlockChainUpdateContext(
				this.accountAnalyzer,
				this.originalAnalyzer,
				this.blockChainLastBlockLayer,
				this.blockDao,
				this.services,
				unconfirmedTransactions,
				dbParentBlock,
				peerChain,
				ourScore,
				hasOwnChain);

		return updateContext.update();
	}

	public BlockLookup createLocalBlockLookup() {
		return new LocalBlockLookupAdapter(
				this.blockDao,
				this.accountAnalyzer.getAccountCache(),
				this.blockChainLastBlockLayer.getLastDbBlock(),
				this.ourScore,
				BlockChainConstants.BLOCKS_LIMIT);
	}
}