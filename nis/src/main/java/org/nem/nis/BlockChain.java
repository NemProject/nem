package org.nem.nis;

import org.nem.core.connect.*;
import org.nem.core.model.Block;
import org.nem.nis.balances.BlockExecutor;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.mappers.TransferMapper;
import org.nem.nis.sync.*;
import org.nem.peer.*;
import org.springframework.beans.factory.annotation.Autowired;

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


	public void analyzeLastBlock(org.nem.nis.dbmodel.Block curBlock) {
		LOGGER.info("analyzing last block: " + Long.toString(curBlock.getShortId()));
		lastBlock = curBlock;
	}

	private void penalize(Node node) {

	}

	interface DbBlockVisitor {
		public void visit(org.nem.nis.dbmodel.Block parentBlock, org.nem.nis.dbmodel.Block dbBlock);
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
		public void visit(org.nem.nis.dbmodel.Block parentBlock, org.nem.nis.dbmodel.Block dbBlock) {
			lastScore = blockScorer.calculateBlockScore(parentBlock, dbBlock);
			partialScore += lastScore;
		}

		public long getScore() {
			// equal to 2*x_0 + x_1 + x_2 + ...
			return partialScore + lastScore;
		}
	}

	private void reverseChainIterator(final long wantedHeight, final DbBlockVisitor[] dbBlockVisitors) {
		long currentHeight = getLastBlockHeight();

		if (currentHeight <= 1 || wantedHeight <= 1) {
			return;
		}

		if (currentHeight == wantedHeight) {
			return;
		}

		org.nem.nis.dbmodel.Block block = blockDao.findByHeight(new BlockHeight(currentHeight));
		while (currentHeight != wantedHeight) {
			org.nem.nis.dbmodel.Block parentBlock = blockDao.findByHeight(new BlockHeight(currentHeight - 1));

			for (DbBlockVisitor dbBlockVisitor : dbBlockVisitors) {
				dbBlockVisitor.visit(parentBlock, block);
			}
			currentHeight--;

			block = parentBlock;
		}
	}

	private void addRevertedTransactionsAsUnconfirmed(final long wantedHeight, final AccountAnalyzer accountAnalyzer) {
		long currentHeight = getLastBlockHeight();

		while (currentHeight != wantedHeight) {
			org.nem.nis.dbmodel.Block block = blockDao.findByHeight(new BlockHeight(currentHeight));

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

	private void dropDbBlocksAfter(final BlockHeight height) {
		blockDao.deleteBlocksAfterHeight(height);

		lastBlock = blockDao.findByHeight(height);
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
	 * @param connectorPool The sync connector pool.
	 * @param node The other node.
	 */
	@Override
	public void synchronizeNode(final SyncConnectorPool connectorPool, final Node node) {
		try {
			this.synchronizeNodeInternal(connectorPool, node);
		} catch (InactivePeerException |FatalPeerException ex) {
			penalize(node);
		}
	}

	private ComparisonResult compareChains(final SyncConnector connector, final Node node) {
		final ComparisonContext context = new ComparisonContext(BLOCKS_LIMIT, REWRITE_LIMIT);
		final BlockChainComparer comparer = new BlockChainComparer(context);

		final BlockLookup remoteLookup = new RemoteBlockLookupAdapter(connector, node);
		final BlockLookup localLookup = new LocalBlockLookupAdapter(this.blockDao, this.accountAnalyzer, this.lastBlock, BLOCKS_LIMIT);

		final ComparisonResult result = comparer.compare(localLookup, remoteLookup);

		if (0 != (ComparisonResult.Code.REMOTE_IS_EVIL & result.getCode())) {
			this.penalize(node);
		}

		return result;
	}

	/**
	 * Reverses transactions between commonBlockHeight and current lastBlock.
	 * Additionally calculates score.
	 *
	 * @param commonBlockHeight height up to which TXes should be reversed.
	 * @param contemporaryAccountAnalyzer AccountLookup upon which reverse should be made
	 *
	 * @return score for iterated blocks.
	 *
	 * WARNING: although it might be tempting to split this function,
	 * please don't. reverseChainIterator accesses blocks in DB,
	 * which will be costly, so it's better to do it in one iteration
	 */
	private long undoTxesAndGetScore(long commonBlockHeight, final AccountAnalyzer contemporaryAccountAnalyzer) {
		final PartialWeightedScoreReversedCalculator chainScore = new PartialWeightedScoreReversedCalculator(scorer);
		reverseChainIterator(commonBlockHeight, new DbBlockVisitor[] {
				chainScore,
				new DbBlockVisitor() {
					@Override
					public void visit(org.nem.nis.dbmodel.Block parentBlock, org.nem.nis.dbmodel.Block dbBlock) {
						BlockExecutor.unapply(contemporaryAccountAnalyzer, dbBlock);
					}
				}
		});

		return chainScore.getScore();
	}


	private void calculatePeerChainDifficulties(Block parentBlock, final List<Block> peerChain) {
		final BlockHeight blockHeight = new BlockHeight(Math.max(1L, parentBlock.getHeight().getRaw() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION + 1));
		final List<TimeInstant> timestamps = blockDao.getTimestampsFrom(blockHeight, (int)BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION);
		final List<BlockDifficulty> difficulties = blockDao.getDifficultiesFrom(blockHeight, (int)BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION);

		for (Block block : peerChain) {
			final BlockDifficulty difficulty = this.scorer.calculateDifficulty(difficulties, timestamps);
			block.setDifficulty(difficulty);

			// apache collections4 only have CircularFifoQueue which as a queue doesn't have .get()
			difficulties.add(difficulty);
			timestamps.add(block.getTimeStamp());
			if (difficulties.size() > BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION) {
				difficulties.remove(0);
				timestamps.remove(0);
			}
		}
	}

	private void calculatePeerChainGenerations(Block parentBlock, final List<Block> peerChain) {
		for (Block block : peerChain) {
			block.setGenerationHash(HashUtils.nextHash(parentBlock.getGenerationHash(), block.getSigner().getKeyPair().getPublicKey()));

			parentBlock = block;
		}
	}

	/**
	 * Validates blocks in peerChain.
	 *
	 * @param contemporaryAccountAnalyzer AccountLookup upon which TXes from peerChain should be applied.
	 * @param parentDbBlock parent db block
	 * @param peerChain analyzed fragment of peer's blockchain.
	 *
	 * @return score or -1 if chain is invalid
	 */
	private long validatePeerChainAndGetScore(AccountAnalyzer contemporaryAccountAnalyzer, org.nem.nis.dbmodel.Block parentDbBlock, List<Block> peerChain) {
		final Block parentBlock = BlockMapper.toModel(parentDbBlock, contemporaryAccountAnalyzer);

		final BlockChainValidator validator = new BlockChainValidator(this.scorer, BLOCKS_LIMIT);
		calculatePeerChainDifficulties(parentBlock, peerChain);
		calculatePeerChainGenerations(parentBlock, peerChain);
		if (!validator.isValid(parentBlock, peerChain)) {
			return -1L;
		}
		return validator.computePartialScore(parentBlock, peerChain);
	}


	private void updateOurChain(long commonBlockHeight, AccountAnalyzer contemporaryAccountAnalyzer, List<Block> peerChain, boolean hasOwnChain) {
		//region update our chain
		synchronized (this) {
			accountAnalyzer.replace(contemporaryAccountAnalyzer);

			if (hasOwnChain) {
				// mind that we're using "new" (replaced) accountAnalyzer
				addRevertedTransactionsAsUnconfirmed(commonBlockHeight, accountAnalyzer);
			}

			dropDbBlocksAfter(new BlockHeight(commonBlockHeight));
		}

		for (Block peerBlock : peerChain) {
			if (addBlockToDb(peerBlock)) {
				foraging.removeFromUnconfirmedTransactions(peerBlock);
			}
		}
		//endregion

	}

	private void synchronizeNodeInternal(final SyncConnectorPool connectorPool, final Node node) {
		final AccountAnalyzer contemporaryAccountAnalyzer = new AccountAnalyzer(this.accountAnalyzer);
		final SyncConnector connector = connectorPool.getSyncConnector(contemporaryAccountAnalyzer);
		final ComparisonResult result = compareChains(connector, node);

		if (ComparisonResult.Code.REMOTE_IS_NOT_SYNCED != result.getCode()) {
			return;
		}

		final BlockHeight commonBlockHeight = new BlockHeight(result.getCommonBlockHeight());

		//region revert TXes inside contemporaryAccountAnalyzer
		long ourScore = 0L;
		if (!result.areChainsConsistent()) {

			ourScore = undoTxesAndGetScore(commonBlockHeight.getRaw(), contemporaryAccountAnalyzer);
		}
		//endregion


		//region verify peer's chain
		final org.nem.nis.dbmodel.Block ourDbBlock = blockDao.findByHeight(commonBlockHeight);
		final List<Block> peerChain = connector.getChainAfter(node.getEndpoint(), commonBlockHeight);

		// do not trust peer, take first block from our db and convert it
		long peerScore = validatePeerChainAndGetScore(contemporaryAccountAnalyzer, ourDbBlock, peerChain);
		if (peerScore < 0L) {
			penalize(node);
			return;
		}

		LOGGER.info("our score: " + Long.toString(ourScore) + " peer's score: " + Long.toString(peerScore));

		if (peerScore < ourScore) {
			// we could get peer's score upfront, if it mismatches with
			// what we calculated, we could penalize peer.
			return;
		}

		for (final Block block : peerChain) {
			BlockExecutor.apply(contemporaryAccountAnalyzer, block);
		}
		//endregion

		// mind "not" consistent
		updateOurChain(commonBlockHeight.getRaw(), contemporaryAccountAnalyzer, peerChain, ! result.areChainsConsistent());
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

		final org.nem.nis.dbmodel.Block parent;

		// this method processes only blocks that have been sent directly (pushed)
		// to us, so we can add quite strict rule here
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		if (block.getTimeStamp().compareTo(currentTime.addMinutes(3)) > 0) {
			return false;
		}

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

		// TODO: we should have some time limit set
//		if (block.getTimeStamp() > parent.getTimestamp() + 20*30) {
//			return false;
//		}

		final AccountAnalyzer contemporaryAccountAnalyzer = new AccountAnalyzer(accountAnalyzer);
		long ourScore = 0L;
		boolean hasOwnChain = false;
		// we have parent, check if it has child
		if (parent.getNextBlockId() != null) {
			ourScore = undoTxesAndGetScore(parent.getHeight(), contemporaryAccountAnalyzer);
			hasOwnChain = true;
		}

		final ArrayList<Block> peerChain = new ArrayList<>(1);
		peerChain.add(block);

		long peerscore = validatePeerChainAndGetScore(contemporaryAccountAnalyzer, parent, peerChain);
		if (peerscore < 0) {
			// penalty?
			return false;
		}

		if (peerscore < ourScore) {
			return false;
		}

		updateOurChain(parent.getHeight(), contemporaryAccountAnalyzer, peerChain, hasOwnChain);
		return true;
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