package org.nem.nis.sync;

import org.nem.core.model.*;
import org.nem.nis.BlockScorer;
import org.nem.nis.dao.BlockDao;
import org.nem.peer.Node;
import org.nem.peer.SyncConnector;

import java.util.Arrays;

/**
 * Helper class for synchronizing a block chain.
 */
public class BlockSync {

	public static final int ESTIMATED_BLOCKS_PER_DAY = 1440;

	public static final int BLOCKS_LIMIT = ESTIMATED_BLOCKS_PER_DAY;

	public static final int REWRITE_LIMIT = (ESTIMATED_BLOCKS_PER_DAY / 2);

	private final org.nem.nis.dbmodel.Block lastBlock;
	private final BlockScorer scorer;
	private final BlockDao blockDao;

	public BlockSync(final org.nem.nis.dbmodel.Block lastBlock, final BlockDao blockDao, final BlockScorer scorer) {
		this.lastBlock = lastBlock;
		this.blockDao = blockDao;
		this.scorer = scorer;
	}

	private boolean synchronizeCompareBlocks(Block peerLastBlock, org.nem.nis.dbmodel.Block dbBlock) {
		if (peerLastBlock.getHeight() == dbBlock.getHeight()) {
			if (HashUtils.calculateHash(peerLastBlock).equals(dbBlock.getBlockHash())) {
				if (Arrays.equals(peerLastBlock.getSignature().getBytes(), dbBlock.getForgerProof())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Retrieves last block from another peer and checks if it's different than ours.
	 *
	 * @param connector
	 * @param node
	 *
	 * @return peer's last block or null
	 */
	private Block checkLastBlock(SyncConnector connector, Node node) {
		Block peerLastBlock = connector.getLastBlock(node.getEndpoint());
		if (peerLastBlock == null) {
			return null;
		}

		if (this.synchronizeCompareBlocks(peerLastBlock, this.lastBlock)) {
			return null;
		}

		if (! peerLastBlock.verify()) {
//			penalize(node);
			return null;
		}
		return peerLastBlock;
	}

	private Block synchronize1CompareLastBlock(final SyncConnector connector, final Node node) {
		//region compare last block
		final Block peerLastBlock = checkLastBlock(connector, node);
		if (peerLastBlock == null) {
			return null;
		}
		final long val = peerLastBlock.getHeight() - this.lastBlock.getHeight();

		/* if node is far behind, reject it, not to allow too deep
		   rewrites of blockchain... */
		if (val <= -REWRITE_LIMIT) {
			return null;
		}

		return peerLastBlock;
		//endregion
	}


	private SynchronizeContext synchronize2FindCommonBlock(SyncConnector connector, Node node) {
		//region compare hash chains
		final long startingPoint = Math.max(1, this.lastBlock.getHeight() - REWRITE_LIMIT);
		final HashChain peerHashes = connector.getHashesFrom(node.getEndpoint(), startingPoint);
		if (peerHashes.size() > BLOCKS_LIMIT) {
//			penalize(node);
			return null;
		}

		final HashChain ourHashes = new HashChain(blockDao.getHashesFrom(startingPoint, BLOCKS_LIMIT));
		int i = ourHashes.findFirstDifferent(peerHashes);

		// at least first compared block should be the same, if not, he's a lier or on a fork
		if (i == 0) {
//			penalize(node);
			return null;
		}

		// nothing to do, we have all of peers blocks
		if (i == peerHashes.size()) {
			return null;
		}
		SynchronizeContext synchronizeContext = new SynchronizeContext(startingPoint + i - 1,  ourHashes.size() > i);
		return synchronizeContext;
		//endregion
	}

	/**
	 * Compares given peerBlock, with block from db at height.
	 *
	 * @param node current peer.
	 * @param peerBlock block to compare
	 * @param commonHeight height at which we do comparison
	 * @return true in peer's block has better score, false otherwise
	 */
	private boolean sychronizeCompareAt(Node node, Block peerBlock, long commonHeight) {
		if (!peerBlock.verify()) {
//			penalize(node);
			return false;
		}

		org.nem.nis.dbmodel.Block commonBlock = blockDao.findByHeight(commonHeight);

		org.nem.nis.dbmodel.Block ourBlock = blockDao.findByHeight(commonHeight + 1);

		if (synchronizeCompareBlocks(peerBlock, ourBlock)) {
			return false;
		}

		long peerScore = this.scorer.calculateBlockScore(commonBlock.getBlockHash(), peerBlock.getSigner().getKeyPair().getPublicKey());
		long ourScore = this.scorer.calculateBlockScore(commonBlock.getBlockHash(), ourBlock.getForger().getPublicKey());
		return peerScore < ourScore;
	}

	public SynchronizeContext synchronize(final SyncConnector connector, final Node node) {
		Block peerLastBlock = synchronize1CompareLastBlock(connector, node);
		if (peerLastBlock == null) {
			return null;
		}

		SynchronizeContext context = synchronize2FindCommonBlock(connector, node);
		final long commonBlockHeight = context.commonBlockHeight;
		// not to waste our time, first try to get first block that differs
		final Block differBlock = connector.getBlockAt(node.getEndpoint(), commonBlockHeight + 1);
		if (context.hasOwnChain) {
			if (! this.sychronizeCompareAt(node, differBlock, commonBlockHeight)) {
				return null;
			}
		}

		return context;
	}
}
