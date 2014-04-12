package org.nem.nis;

import org.nem.nis.balances.Balance;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;
import org.nem.core.model.*;
import org.nem.core.model.Block;
import org.nem.core.time.TimeInstant;
import org.nem.nis.mappers.TransferMapper;
import org.nem.nis.sync.*;
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


	public void analyzeLastBlock(org.nem.nis.dbmodel.Block curBlock) {
		LOGGER.info("analyzing last block: " + Long.toString(curBlock.getShortId()));
		lastBlock = curBlock;
	}

	private void penalize(Node node) {

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

	private ComparisonResult compareChains(final SyncConnector connector, final Node node) {		final ComparisonContext context = new ComparisonContext(BLOCKS_LIMIT, REWRITE_LIMIT, this.scorer);
		final BlockChainComparer comparer = new BlockChainComparer(context);

		final BlockLookup remoteLookup = new RemoteBlockLookupAdapter(connector, node);
		final BlockLookup localLookup = new LocalBlockLookupAdapter(this.blockDao, this.accountAnalyzer, this.lastBlock, BLOCKS_LIMIT);

		final ComparisonResult result = comparer.compare(localLookup, remoteLookup);

		if (0 != (ComparisonResult.Code.REMOTE_IS_EVIL & result.getCode())) {
			this.penalize(node);
		}

		return result;
	}

	private void synchronizeNodeInternal(final SyncConnector connector, final Node node) {
		final ComparisonResult result = compareChains(connector, node);

		if (ComparisonResult.Code.REMOTE_IS_NOT_SYNCED != result.getCode()) {
			return;
		}

		final long commonBlockHeight = result.getCommonBlockHeight();

		//region revert TXes inside contemporaryAccountAnalyzer
		final AccountAnalyzer contemporaryAccountAnalyzer = new AccountAnalyzer(accountAnalyzer);
		long ourScore = 0L;
		if (!result.areChainsConsistent()) {

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

		for (final Block block : peerChain) {
			Balance.apply(contemporaryAccountAnalyzer, block);
		}

		//endregion

		//region update our chain
		accountAnalyzer.replace(contemporaryAccountAnalyzer);

		if (!result.areChainsConsistent()) {
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