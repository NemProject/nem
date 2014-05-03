package org.nem.nis;

import org.nem.core.connect.*;
import org.nem.core.model.Block;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.mappers.TransferMapper;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.sync.*;
import org.nem.nis.visitors.*;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.node.Node;
import org.nem.peer.node.NodeApiId;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BlockChain implements BlockSynchronizer, Runnable, Closeable {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());

	public static final int ESTIMATED_BLOCKS_PER_DAY = 1440;

	public static final int BLOCKS_LIMIT = ESTIMATED_BLOCKS_PER_DAY;

	public static final int REWRITE_LIMIT = (ESTIMATED_BLOCKS_PER_DAY / 2);

	private AccountDao accountDao;

	private BlockChainLastBlockLayer blockChainLastBlockLayer;

	private BlockDao blockDao;

	private AccountAnalyzer accountAnalyzer;

	private Foraging foraging;

	private NisPeerNetworkHost host;

	private final BlockScorer scorer = new BlockScorer();

	private final ScheduledThreadPoolExecutor blockGeneratorExecutor;


	public BlockChain() {
		this.blockGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
	}

	public void boot() {
		this.blockGeneratorExecutor.scheduleWithFixedDelay(this, 5, 3, TimeUnit.SECONDS);
	}

	@Override
	public void close() {
		this.blockGeneratorExecutor.shutdownNow();
	}

	@Override
	public void run() {
		Block block = this.foraging.forageBlock();
		if (block != null) {
			// make a full-blown analysis
			// TODO: we can call it thanks to the "hack" inside processBlock
			if (this.processBlock(block)) {
				// TODO: this probably should be called directly inside processBlock()
				host.getNetwork().broadcast(NodeApiId.REST_PUSH_BLOCK, block);
			}
		}
	}

	@Autowired
	public void setAccountDao(AccountDao accountDao) { this.accountDao = accountDao; }

	@Autowired
	public void setAccountAnalyzer(AccountAnalyzer accountAnalyzer) { this.accountAnalyzer = accountAnalyzer; }

	@Autowired
	public void setBlockDao(BlockDao blockDao) { this.blockDao = blockDao; }

	@Autowired
	public void setForaging(Foraging foraging) { this.foraging = foraging; }

	@Autowired
	public void setBlockChainLastBlockLayer(BlockChainLastBlockLayer blockChainLastBlockLayer) { this.blockChainLastBlockLayer = blockChainLastBlockLayer; }

	@Autowired
	public void setNetworkHost(final NisPeerNetworkHost host) { this.host = host; }

	private void penalize(Node node) {

	}

	private void addRevertedTransactionsAsUnconfirmed(Set<Hash> transactionHashes, final long wantedHeight, final AccountAnalyzer accountAnalyzer) {
		long currentHeight = blockChainLastBlockLayer.getLastBlockHeight();

		while (currentHeight != wantedHeight) {
			org.nem.nis.dbmodel.Block block = blockDao.findByHeight(new BlockHeight(currentHeight));

			// if the transaction is in DB it means at some point
			// isValid and verify had to be called on it, so we can safely add it
			// as unconfirmed
			block.getBlockTransfers().stream()
					.filter(tr -> ! transactionHashes.contains(tr))
					.map(tr -> TransferMapper.toModel(tr, accountAnalyzer))
					.forEach(tr -> foraging.addUnconfirmedTransactionWithoutDbCheck(tr));
			currentHeight--;
		}
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
		} catch (InactivePeerException | FatalPeerException ex) {
			penalize(node);
		}
	}

	private ComparisonResult compareChains(final SyncConnector connector, final Node node) {
		final ComparisonContext context = new ComparisonContext(BLOCKS_LIMIT, REWRITE_LIMIT);
		final BlockChainComparer comparer = new BlockChainComparer(context);

		final BlockLookup remoteLookup = new RemoteBlockLookupAdapter(connector, node);
		final BlockLookup localLookup = this.createLocalBlockLookup(this.accountAnalyzer);

		final ComparisonResult result = comparer.compare(localLookup, remoteLookup);

		if (0 != (ComparisonResult.Code.REMOTE_IS_EVIL & result.getCode())) {
			this.penalize(node);
		}

		return result;
	}

	private BlockLookup createLocalBlockLookup(final AccountAnalyzer currentAccountAnalyzer) {
		return new LocalBlockLookupAdapter(this.blockDao, currentAccountAnalyzer, this.blockChainLastBlockLayer.getLastDbBlock(), BLOCKS_LIMIT);
	}

	/**
	 * Reverses transactions between commonBlockHeight and current lastBlock.
	 * Additionally calculates score.
	 *
	 * @param commonBlockHeight height up to which TXes should be reversed.
	 *
	 * @return score for iterated blocks.
	 */
	private long undoTxesAndGetScore(AccountAnalyzer contemporaryAccountAnalyzer, long commonBlockHeight) {
		final PartialWeightedScoreVisitor scoreVisitor = new PartialWeightedScoreVisitor(
				this.scorer,
				PartialWeightedScoreVisitor.BlockOrder.Reverse);

		// this is delicate and the order matters, first visitor during unapply changes amount of foraged blocks
		// second visitor needs that information
		final List<BlockVisitor> visitors = new ArrayList<>();
		visitors.add(new UndoBlockVisitor());
		visitors.add(scoreVisitor);
		final BlockVisitor visitor = new AggregateBlockVisitor(visitors);
		BlockIterator.unwindUntil(this.createLocalBlockLookup(contemporaryAccountAnalyzer), new BlockHeight(commonBlockHeight), visitor);

		return scoreVisitor.getScore();
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

	/**
	 * Validates blocks in peerChain.
	 *
	 * @param parentBlock parent block
	 * @param peerChain analyzed fragment of peer's blockchain.
	 *
	 * @return score or -1 if chain is invalid
	 */
	private boolean validatePeerChain(final Block parentBlock, final List<Block> peerChain) {
		final BlockChainValidator validator = new BlockChainValidator(this.scorer, BLOCKS_LIMIT);
		calculatePeerChainDifficulties(parentBlock, peerChain);
		return validator.isValid(parentBlock, peerChain);
	}

	private long getPeerChainScore(final Block parentBlock, final List<Block> peerChain) {
		final PartialWeightedScoreVisitor scoreVisitor = new PartialWeightedScoreVisitor(
				this.scorer,
				PartialWeightedScoreVisitor.BlockOrder.Forward);
		BlockIterator.all(parentBlock, peerChain, scoreVisitor);
		return scoreVisitor.getScore();
	}

	/*
	 * 1. execute all blocks
	 * 2. replace current accountAnalyzer with contemporaryAccountAnalyzer
	 * 3. add unconfirmed transactions from "our" chain
	 *    (except those transactions, that are included in peer's chain)
	 *
	 * 4. drop "our" blocks from the db
	 *
	 * 5. update db with "peer's" chain
	 */
	private void updateOurChain(long commonBlockHeight, AccountAnalyzer contemporaryAccountAnalyzer, List<Block> peerChain, boolean hasOwnChain) {

		for (final Block block : peerChain) {
			block.execute();
		}

		synchronized (blockChainLastBlockLayer) {
			contemporaryAccountAnalyzer.shallowCopyTo(this.accountAnalyzer);

			if (hasOwnChain) {
				// mind that we're using "new" (replaced) accountAnalyzer
				Set<Hash> transactionHashes = peerChain.stream().flatMap(bl -> bl.getTransactions().stream()).map(HashUtils::calculateHash).collect(Collectors.toSet());
				addRevertedTransactionsAsUnconfirmed(transactionHashes, commonBlockHeight, accountAnalyzer);
			}

			blockChainLastBlockLayer.dropDbBlocksAfter(new BlockHeight(commonBlockHeight));
		}

		peerChain.stream().filter(blockChainLastBlockLayer::addBlockToDb).forEach(foraging::removeFromUnconfirmedTransactions);
	}

	private void synchronizeNodeInternal(final SyncConnectorPool connectorPool, final Node node) {
		final AccountAnalyzer contemporaryAccountAnalyzer = this.accountAnalyzer.copy();
		final SyncConnector connector = connectorPool.getSyncConnector(contemporaryAccountAnalyzer);
		final ComparisonResult result = compareChains(connector, node);

		if (ComparisonResult.Code.REMOTE_IS_NOT_SYNCED != result.getCode()) {
			return;
		}

		final BlockHeight commonBlockHeight = new BlockHeight(result.getCommonBlockHeight());

		//region revert TXes inside contemporaryAccountAnalyzer
		long ourScore = 0L;
		if (!result.areChainsConsistent()) {
			ourScore = undoTxesAndGetScore(contemporaryAccountAnalyzer, commonBlockHeight.getRaw());
		}
		//endregion

		//region verify peer's chain
		final org.nem.nis.dbmodel.Block ourDbBlock = blockDao.findByHeight(commonBlockHeight);
		final List<Block> peerChain = connector.getChainAfter(node.getEndpoint(), commonBlockHeight);

		updateOurChain(ourDbBlock, contemporaryAccountAnalyzer, peerChain, ourScore, !result.areChainsConsistent());
	}

	/**
	 * Checks if passed receivedBlock is correct, and if eligible adds it to db
	 *
	 * @param receivedBlock - receivedBlock that's going to be processed
	 *
	 * @return false if receivedBlock was known or invalid, true if ok and added to db
	 */
	public boolean processBlock(Block receivedBlock) {
		final Hash blockHash = HashUtils.calculateHash(receivedBlock);
		final Hash parentHash = receivedBlock.getPreviousBlockHash();

		final org.nem.nis.dbmodel.Block parent;

		// this method processes only blocks that have been sent directly (pushed)
		// to us, so we can add quite strict rule here
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		if (receivedBlock.getTimeStamp().compareTo(currentTime.addMinutes(3)) > 0) {
			return false;
		}

		// receivedBlock already seen
		synchronized (blockChainLastBlockLayer) {
			if (blockDao.findByHash(blockHash) != null) {
				return false;
			}

			// check if we know previous receivedBlock
			parent = blockDao.findByHash(parentHash);
		}

		// if we don't have parent, we can't do anything with this receivedBlock
		if (parent == null) {
			return false;
		}

		// TODO: we should have some time limit set
//		if (receivedBlock.getTimeStamp() > parent.getTimestamp() + 20*30) {
//			return false;
//		}

		final AccountAnalyzer contemporaryAccountAnalyzer = this.accountAnalyzer.copy();
		// EVIL hack, see issue#70
		org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(receivedBlock, new AccountDaoLookupAdapter(this.accountDao));
		receivedBlock = BlockMapper.toModel(dbBlock, contemporaryAccountAnalyzer);
		// EVIL hack end

		long ourScore = 0L;
		boolean hasOwnChain = false;
		// we have parent, check if it has child
		if (parent.getNextBlockId() != null) {
			// warning: this changes number of foraged blocks
			ourScore = undoTxesAndGetScore(contemporaryAccountAnalyzer, parent.getHeight());
			hasOwnChain = true;
		}

		final ArrayList<Block> peerChain = new ArrayList<>(1);
		peerChain.add(receivedBlock);

		return updateOurChain(parent, contemporaryAccountAnalyzer, peerChain, ourScore, hasOwnChain);
	}

	private boolean updateOurChain(
			final org.nem.nis.dbmodel.Block parent,
			final AccountAnalyzer contemporaryAccountAnalyzer,
			final List<Block> peerChain,
			final long ourScore,
			final boolean hasOwnChain) {

		// do not trust peer, take first block from our db and convert it
		final Block parentBlock = BlockMapper.toModel(parent, contemporaryAccountAnalyzer);
		if (!validatePeerChain(parentBlock, peerChain)) {
			// penalty?
			return false;
		}

		// warning: this changes number of foraged blocks
		long peerScore = getPeerChainScore(parentBlock, peerChain);
		if (peerScore < 0) {
			// penalty?
			return false;
		}

		LOGGER.info("our score: " + Long.toString(ourScore) + " peer's score: " + Long.toString(peerScore));
		if (peerScore < ourScore) {
			// we could get peer's score upfront, if it mismatches with
			// what we calculated, we could penalize peer.
			return false;
		}

		updateOurChain(parent.getHeight(), contemporaryAccountAnalyzer, peerChain, hasOwnChain);
		return true;
	}
}