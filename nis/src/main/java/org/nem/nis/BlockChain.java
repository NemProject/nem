package org.nem.nis;

import org.nem.core.crypto.PublicKey;
import org.nem.nis.balances.Balance;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;
import org.nem.core.model.*;
import org.nem.core.model.Block;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ByteUtils;
import org.nem.nis.mappers.TransferMapper;
import org.nem.peer.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

public class BlockChain implements BlockSynchronizer {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());

	public static final int ESTIMATED_BLOCKS_PER_DAY = 1440;

	public static final int BLOCKS_LIMIT = ESTIMATED_BLOCKS_PER_DAY;

	public static final int REWRITE_LIMIT = (ESTIMATED_BLOCKS_PER_DAY / 2);

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	@Autowired
	private NisPeerNetworkHost host;

	@Autowired
	private Foraging foraging;

	// for now it's easier to keep it like this
	private org.nem.nis.dbmodel.Block lastBlock;

	private final BlockScorer scorer = new BlockScorer();

	public BlockChain() {
	}

	public void bootup() {
		LOGGER.info("booting up block generator");
	}

	public org.nem.nis.dbmodel.Block getLastDbBlock() {
		return lastBlock;
	}

	private Long getLastBlockHeight() {
		return lastBlock.getHeight();
	}

	private long calcDbBlockScore(Hash parentHash, org.nem.nis.dbmodel.Block block) {
		long r1 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(block.getForger().getPublicKey().getRaw(), 10, 14)));
		long r2 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(parentHash.getRaw(), 10, 14)));

		return r1 + r2;
	}

	private long calcBlockScore(Hash parentHash, Block block) {
		long r1 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(block.getSigner().getKeyPair().getPublicKey().getRaw(), 10, 14)));
		long r2 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(parentHash.getRaw(), 10, 14)));

		return r1 + r2;
	}

	public void analyzeLastBlock(org.nem.nis.dbmodel.Block curBlock) {
		LOGGER.info("analyzing last block: " + Long.toString(curBlock.getShortId()));
		lastBlock = curBlock;
	}


	public boolean synchronizeCompareBlocks(Block peerLastBlock, org.nem.nis.dbmodel.Block dbBlock) {
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
	 * Compares given peerBlock, with block from db at height.
	 *
	 * @param node current peer.
	 * @param peerBlock block to compare
	 * @param commonHeight height at which we do comparison
	 * @return true in peer's block has better score, false otherwise
	 */
	public boolean sychronizeCompareAt(Node node, Block peerBlock, long commonHeight) {
		if (!peerBlock.verify()) {
			penalize(node);
			return false;
		}

		org.nem.nis.dbmodel.Block commonBlock = blockDao.findByHeight(commonHeight);

		org.nem.nis.dbmodel.Block ourBlock = blockDao.findByHeight(commonHeight + 1);

		if (synchronizeCompareBlocks(peerBlock, ourBlock)) {
			return false;
		}

		long peerScore = calcBlockScore(commonBlock.getBlockHash(), peerBlock);
		long ourScore = calcDbBlockScore(commonBlock.getBlockHash(), ourBlock);
		if (peerScore < ourScore) {
			return true;
		}

		return false;
	}

	private void penalize(Node node) {

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

		if (this.synchronizeCompareBlocks(peerLastBlock, lastBlock)) {
			return null;
		}

		if (! peerLastBlock.verify()) {
			penalize(node);
			return null;
		}
		return peerLastBlock;
	}

	interface DbBlockVisitor {
		public void visit(org.nem.nis.dbmodel.Block dbBlock);
	};

	class PartialWeightedScoreReversedCalculator implements DbBlockVisitor {
		private final BlockScorer blockScorer;
		private long lastScore;
		private long partialScore;

		public PartialWeightedScoreReversedCalculator(BlockScorer scorer) {
			blockScorer = scorer;
			lastScore = 0L;
			partialScore = 0L;
		}

		// visit should be called at most 1440 times, every score fits in 32-bits
		// so long will be enough to keep partial score
		@Override
		public void visit(org.nem.nis.dbmodel.Block dbBlock) {
			lastScore = blockScorer.calculateBlockScore(dbBlock.getPrevBlockHash(), dbBlock.getForger().getPublicKey());
			partialScore += lastScore;
		}

		public long getScore() {
			// equal to 2*x_0 + x_1 + x_2 + ...
			return partialScore + lastScore;
		}
	}

	private void reverseChainIterator(final long wantedHeight, final DbBlockVisitor[] dbBlockVisitors) {
		long currentHeight = getLastBlockHeight();

		while (currentHeight != wantedHeight) {
			org.nem.nis.dbmodel.Block block = blockDao.findByHeight(currentHeight);
			for (DbBlockVisitor dbBlockVisitor : dbBlockVisitors) {
				dbBlockVisitor.visit(block);
			}
			currentHeight--;
		}
	}

	private void addRevertedTransactionsAsUnconfirmed(final long wantedHeight, final AccountAnalyzer accountAnalyzer) {
		long currentHeight = getLastBlockHeight();

		while (currentHeight != wantedHeight) {
			org.nem.nis.dbmodel.Block block = blockDao.findByHeight(currentHeight);

			// if the transaction is in DB it means at some point
			// isValid and verify had to be called on it, so we can safely add it
			// as unconfirmed
			for (Transfer transfer : block.getBlockTransfers()) {
				// block is still in db
				foraging.addUnconfirmedTransactionWithoutDbCheck(TransferMapper.toModel(transfer, accountAnalyzer));
			}
			currentHeight--;
		}
	}

	private void dropDbBlocksAfter(long height) {
		blockDao.deleteBlocksAfterHeight(height);
	}

	/**
	 * Synch algorithm:
	 *  1. Get peer's last block compare with ours, assuming it's ok
	 *  2. Take hashes of last blocks - at most REWRITE_LIMIT hashes, compare with proper hashes
	 *     of peer, to find last common and first different block.
	 *     If all peer's hashes has been checked we have nothing to do
	 *  3. if we have some blocks left AFTER common blocks, we'll need to revert those transactions,
	 *     but before that we'll do some simple check, to see if peer's chain is actually better
	 *  4. Now we can get peer's chain and verify it
	 *  5. Once we've verified it, we can apply it
	 *     (all-or-nothing policy, if verification failed, we won't try to apply part of it)
	 *
	 * @param connector
	 * @param node
	 */
	@Override
	public void synchronizeNode(final SyncConnector connector, final Node node) {
		try {
			this.synchronizeNodeInternal(connector, node);
		} catch (InactivePeerException|FatalPeerException ex) {
			penalize(node);
		}
	}

	private Block synchronize1CompareLastBlock(final SyncConnector connector, final Node node) {
		//region compare last block
		final Block peerLastBlock = checkLastBlock(connector, node);
		if (peerLastBlock == null) {
			return null;
		}
		final long val = peerLastBlock.getHeight() - this.getLastBlockHeight();

		/* if node is far behind, reject it, not to allow too deep
		   rewrites of blockchain... */
		if (val <= -REWRITE_LIMIT) {
			return null;
		}

		return peerLastBlock;
		//endregion
	}

	class SynchronizeContext {
		public long commonBlockHeight;
		public boolean hasOwnChain;

		SynchronizeContext(long commonBlockHeight, boolean hasOwnChain) {
			this.commonBlockHeight = commonBlockHeight;
			this.hasOwnChain = hasOwnChain;
		}
	}

	private SynchronizeContext synchronize2FindCommonBlock(SyncConnector connector, Node node) {
		//region compare hash chains
		final long startingPoint = Math.max(1, this.getLastBlockHeight() - REWRITE_LIMIT);
		final HashChain peerHashes = connector.getHashesFrom(node.getEndpoint(), startingPoint);
		if (peerHashes.size() > BLOCKS_LIMIT) {
			penalize(node);
			return null;
		}

		final HashChain ourHashes = new HashChain(blockDao.getHashesFrom(startingPoint, BLOCKS_LIMIT));
		int i = ourHashes.findFirstDifferent(peerHashes);

		// at least first compared block should be the same, if not, he's a lier or on a fork
		if (i == 0) {
			penalize(node);
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

	private void synchronizeNodeInternal(final SyncConnector connector, final Node node) {
		Block peerLastBlock = synchronize1CompareLastBlock(connector, node);
		if (peerLastBlock == null) {
			return;
		}

		SynchronizeContext synchronizeContext = synchronize2FindCommonBlock(connector, node);
		if (synchronizeContext == null) {
			return;
		}
		final long commonBlockHeight = synchronizeContext.commonBlockHeight;

		//region revert TXes inside contemporaryAccountAnalyzer
		final AccountAnalyzer contemporaryAccountAnalyzer = new AccountAnalyzer(accountAnalyzer);
		long ourScore = 0L;
		if (synchronizeContext.hasOwnChain) {
			// not to waste our time, first try to get first block that differs
			final Block differBlock = connector.getBlockAt(node.getEndpoint(), commonBlockHeight + 1);

			if (! this.sychronizeCompareAt(node, differBlock, commonBlockHeight)) {
				return;
			}

			PartialWeightedScoreReversedCalculator chainScore = new PartialWeightedScoreReversedCalculator(scorer);
			reverseChainIterator(commonBlockHeight, new DbBlockVisitor[]{
					chainScore,
					new DbBlockVisitor() {
						@Override
						public void visit(org.nem.nis.dbmodel.Block dbBlock) {
							Balance.unapply(contemporaryAccountAnalyzer, dbBlock);
						}
					}
			});

			ourScore = chainScore.getScore();
		}
		//endregion


		//region verify peer's chain
		final org.nem.nis.dbmodel.Block ourDbBlock = blockDao.findByHeight(commonBlockHeight);
		final List<Block> peerChain = connector.getChainAfter(node.getEndpoint(), commonBlockHeight);

		// do not trust peer, take first block from our db and convert it
		final Block parentBlock = BlockMapper.toModel(ourDbBlock, contemporaryAccountAnalyzer);

		final BlockChainValidator validator = new BlockChainValidator(this.scorer, BLOCKS_LIMIT, contemporaryAccountAnalyzer);
		if (!validator.isValid(parentBlock, peerChain)) {
			penalize(node);
			return;
		}

		long peerScore = validator.computePartialScore(parentBlock, peerChain);

		if (peerScore < ourScore) {
			// we could get peer's score upfront, if it mismatches with
			// what we calculated, we could penalize peer.
			return;
		}

		for (final Block block : peerChain) {
			Balance.apply(contemporaryAccountAnalyzer, block);
		}

		//endregion

		//region update our chain
		accountAnalyzer.replace(contemporaryAccountAnalyzer);

		if (synchronizeContext.hasOwnChain) {
			// mind that we're using "new" (replaced) accountAnalyzer
			addRevertedTransactionsAsUnconfirmed(commonBlockHeight, accountAnalyzer);
		}

		synchronized (this) {
			dropDbBlocksAfter(commonBlockHeight);
		}

		for (Block peerBlock : peerChain) {
			if (addBlockToDb(peerBlock)) {
				foraging.removeFromUnconfirmedTransactions(peerBlock);
			}
		}
		//endregion
	}

	/**
	 * Checks if passed block is correct, and if eligible adds it to db
	 *
	 * @param block - block that's going to be processed
	 *
	 * @return false if block was known or invalid, true if ok and added to db
	 */
	public boolean processBlock(Block block) {
		final Hash blockHash = HashUtils.calculateHash(block);
		final Hash parentHash = block.getPreviousBlockHash();

		org.nem.nis.dbmodel.Block parent;

		// block already seen
		synchronized (this) {
			if (blockDao.findByHash(blockHash) != null) {
				return false;
			}

			// check if we know previous block
			parent = blockDao.findByHash(parentHash);
		}

		// if we don't have parent, we can't do anything with this block
		if (parent == null) {
			return false;
		}

		// we have parent, check if it has child
		if (parent.getNextBlockId() != null) {
			org.nem.nis.dbmodel.Block child = blockDao.findById(parent.getNextBlockId());
			// TODO: compare block score, if analyzed block is better, rollback block(s) from db
			if (child != null) {
				return false;
			}
		}

		// TODO: can't apply it now, cause right now we don't generate empty blocks.
//		if (block.getTimeStamp() > parent.getTimestamp() + 20*30) {
//			return false;
//		}

		// TODO: WARNING: as for now this method processes only blocks
		// that have been sent directly, so we can add quite strict rule here
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		if (block.getTimeStamp().compareTo(currentTime.addMinutes(30)) > 0) {
			return false;
		}

		final Block parentBlock = BlockMapper.toModel(parent, accountAnalyzer);
		final BigInteger hit = this.scorer.calculateHit(parentBlock);
		final BigInteger target = this.scorer.calculateTarget(parentBlock, block);

		if (hit.compareTo(target) >= 0) {
			return false;
		}

		throw new RuntimeException("not yet finished");

		// 1. add block to db
		// 2. remove transactions from unconfirmed transactions.
		// run account analyzer?
	}

	public boolean addBlockToDb(Block bestBlock) {
		synchronized (this) {

			final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(bestBlock, new AccountDaoLookupAdapter(this.accountDao));

			// hibernate will save both block AND transactions
			// as there is cascade in Block
			// mind that there is NO cascade in transaction (near block field)
			blockDao.save(dbBlock);

			lastBlock.setNextBlockId(dbBlock.getId());
			blockDao.updateLastBlockId(lastBlock);

			lastBlock = dbBlock;

		} // synchronized

		return true;
	}
}
