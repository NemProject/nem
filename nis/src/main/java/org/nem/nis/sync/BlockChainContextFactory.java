package org.nem.nis.sync;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.service.BlockChainLastBlockLayer;

import java.util.Collection;

/**
 * Helper class for creating contexts used during block synchronization.
 */
public class BlockChainContextFactory {
	private final ReadOnlyNisCache nisCache;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockDao blockDao;
	private final BlockChainServices services;
	private final UnconfirmedTransactions unconfirmedTransactions;

	public BlockChainContextFactory(
			final ReadOnlyNisCache nisCache,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockDao blockDao,
			final BlockChainServices services,
			final UnconfirmedTransactions unconfirmedTransactions) {
		this.nisCache = nisCache;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockDao = blockDao;
		this.services = services;
		this.unconfirmedTransactions = unconfirmedTransactions;
	}

	/**
	 * Creates a sync context.
	 *
	 * @param localScore The local chain score.
	 */
	public BlockChainSyncContext createSyncContext(final BlockChainScore localScore) {
		return new BlockChainSyncContext(
				this.nisCache,
				this.blockChainLastBlockLayer,
				this.blockDao,
				this.services,
				localScore);
	}

	/**
	 * Creates an update context.
	 *
	 * @param syncContext The sync context.
	 * @param dbParentBlock The parent block (from the database).
	 * @param peerChain The peer chain.
	 * @param localScore The local chain score.
	 * @param hasOwnChain true if the peer chain is inconsistent with the local chain
	 * (i.e. if the peer chain is accepted, parts of the local chain will need to be rolled back).
	 */
	public BlockChainUpdateContext createUpdateContext(
			final BlockChainSyncContext syncContext,
			final org.nem.nis.dbmodel.Block dbParentBlock,
			final Collection<Block> peerChain,
			final BlockChainScore localScore,
			final boolean hasOwnChain) {
		return new BlockChainUpdateContext(
				syncContext.nisCache(),
				this.nisCache,
				this.blockChainLastBlockLayer,
				this.blockDao,
				this.services,
				this.unconfirmedTransactions,
				dbParentBlock,
				peerChain,
				localScore,
				hasOwnChain);
	}
}
