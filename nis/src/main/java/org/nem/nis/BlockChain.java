package org.nem.nis;

import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ByteUtils;
import org.nem.peer.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

public class BlockChain implements BlockSynchronizer {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());
	// 500_000_000 nems have force to generate block every minute
	public static final long MAGIC_MULTIPLIER = 614891469L;
	//
	public static final int ESTIMATED_BLOCKS_PER_DAY = 1440;

    public static final int BLOCKS_LIMIT = (ESTIMATED_BLOCKS_PER_DAY / 2);

    public static final int REWRITE_LIMIT = (ESTIMATED_BLOCKS_PER_DAY / 2);

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	@Autowired
	private NisPeerNetworkHost host;

	// for now it's easier to keep it like this
	org.nem.nis.dbmodel.Block lastBlock;

	public BlockChain() {
	}

	public void bootup() {
		LOGGER.info("booting up block generator");
	}

	public org.nem.nis.dbmodel.Block getLastDbBlock() {
		return lastBlock;
	}

	public byte[] getLastBlockHash() {
		return lastBlock.getBlockHash();
	}

	public Long getLastBlockHeight() {
		return lastBlock.getHeight();
	}

	public byte[] getLastBlockSignature() {
		return lastBlock.getForgerProof();
	}

	public int getLastBlockTimestamp() {
		return lastBlock.getTimestamp();
	}

	public long getLastBlockScore() {
		return calcDbBlockScore(lastBlock);
	}


	private long calcDbBlockScore(org.nem.nis.dbmodel.Block block) {
		long r1 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(block.getForgerProof(), 10, 14)));
		long r2 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(block.getBlockHash(), 10, 14)));

		return r1 + r2;
	}

	private long calcBlockScore(Block block) {
		long r1 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(block.getSignature().getBytes(), 10, 14)));
		long r2 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(HashUtils.calculateHash(block), 10, 14)));

		return r1 + r2;
	}

	public void analyzeLastBlock(org.nem.nis.dbmodel.Block curBlock) {
		LOGGER.info("analyzing last block: " + Long.toString(curBlock.getShortId()));
		lastBlock = curBlock;

		Block block = BlockMapper.toModel(lastBlock, accountAnalyzer);
	}


	public boolean synchronizeCompareBlocks(Block peerLastBlock, org.nem.nis.dbmodel.Block dbBlock) {
		if (peerLastBlock.getHeight() == dbBlock.getHeight()) {
			if (Arrays.equals(HashUtils.calculateHash(peerLastBlock), dbBlock.getBlockHash())) {
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
	 * @param height height at which we do comparison
	 * @return true in peer's block has better score, false otherwise
	 */
	public boolean sychronizeCompareAt(Node node, Block peerBlock, long height) {
		if (!peerBlock.verify()) {
			penalize(node);
			return false;
		}

		org.nem.nis.dbmodel.Block ourBlock = blockDao.findByHeight(height);

		if (synchronizeCompareBlocks(peerBlock, ourBlock)) {
			return false;
		}

		long peerScore = calcBlockScore(peerBlock);
		long ourScore = calcDbBlockScore(ourBlock);
		if (peerScore < ourScore) {
			return true;
		}

		return false;
	}

	private boolean validateBlock(Block block, Block parentBlock, AccountAnalyzer contemporaryAccountAnalyzer) {
		if (block.getTimeStamp().compareTo(parentBlock.getTimeStamp()) < 0) {
			return false;
		}

		Account forgerAccount = contemporaryAccountAnalyzer.findByAddress(block.getSigner().getAddress());
		if (forgerAccount.getBalance().compareTo(Amount.ZERO) < 1) {
			return false;
		}

		BigInteger hit = new BigInteger(1, Arrays.copyOfRange(parentBlock.getSignature().getBytes(), 2, 10));
		TimeInstant blockTimeStamp = block.getTimeStamp();
		long forgerEffectiveBallance = forgerAccount.getBalance().getNumNem();
		BigInteger target = Foraging.calculateTarget(parentBlock.getTimeStamp(), blockTimeStamp, forgerEffectiveBallance);

		if (hit.compareTo(target) >= 0) {
			return false;
		}

		return true;
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

	private void synchronizeNodeInternal(final SyncConnector connector, final Node node) {
		//region step 1
		Block peerLastBlock = checkLastBlock(connector, node);
		if (peerLastBlock == null) {
			return;
		}
		long val = peerLastBlock.getHeight() - this.getLastBlockHeight();

		// if node is far behind, reject it, not to allow too deep
		// rewrites of blockchain...
		if (val < -REWRITE_LIMIT) {
			return;
		}
		//endregion

		//region step 2
		long startingPoint = Math.max(1, this.getLastBlockHeight() - REWRITE_LIMIT);
		List<ByteArray> peerHashes = connector.getHashesFrom(node.getEndpoint(), startingPoint);

        if (peerHashes.size() > BLOCKS_LIMIT) {
            penalize(node);
            return;
        }

        List<byte[]> ourHashes = blockDao.getHashesFrom(startingPoint, BLOCKS_LIMIT);
		int limit = Math.min(ourHashes.size(), peerHashes.size());
		int i;
		for (i = 0; i < limit; ++i) {
			if (!Arrays.equals(peerHashes.get(i).get(), ourHashes.get(i))) {
				break;
			}
		}
		// at least first compared block should be the same, if not, he's a lier or on a fork
		if (i == 0) {
			penalize(node);
			return;
		}

		// nothing to do, we have all of peers blocks
		if (i == peerHashes.size()) {
			return;
		}
		//endregion

		//region step 3
		long commonBlockHeight = startingPoint + i - 1;
		AccountAnalyzer contemporaryAccountAnalyzer = new AccountAnalyzer(accountAnalyzer);
		if (ourHashes.size() > i) {
			// not to waste our time, first try to get first block that differs
			long diffBlockHeight = commonBlockHeight + 1;
			Block commonBlock = connector.getBlockAt(node.getEndpoint(), diffBlockHeight);

			if (! this.sychronizeCompareAt(node, commonBlock, diffBlockHeight)) {
				return;
			}

			// TODO: revert transactions (balances) "in" contemporaryAccountAnalyzer
			LOGGER.severe("virtual chain not handled yet");
			System.exit(-1);
		}
		//endregion

		//region step 4
		org.nem.nis.dbmodel.Block ourDbBlock = blockDao.findByHeight(commonBlockHeight);
		List<Block> peerChain = connector.getChainAfter(node.getEndpoint(), commonBlockHeight);

		if (peerChain.size() > BLOCKS_LIMIT) {
			penalize(node);
			return;
		}

		// do not trust peer, take first block from our db and convert it
		Block parentBlock = BlockMapper.toModel(ourDbBlock, contemporaryAccountAnalyzer);

		long wantedHeight = commonBlockHeight + 1;
		for (Block block : peerChain) {
			if (block.getHeight() != wantedHeight ||
					!block.verify() ||
					!validateBlock(block, parentBlock, contemporaryAccountAnalyzer)) {
				penalize(node);
				return;
			}

			for (Transaction transaction : block.getTransactions()) {
				if (!transaction.isValid()) {
					penalize(node);
					return;
				}
				if (!transaction.verify()) {
					penalize(node);
					return;
				}
			}

			// TODO: need too apply transactions here (on contemporaryAccountAnalyzer),
			// to have proper data for next iteration

			parentBlock = block;

			wantedHeight += 1;
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
		byte[] blockHash = HashUtils.calculateHash(block);
		byte[] parentHash = block.getPreviousBlockHash();

		org.nem.nis.dbmodel.Block parent;

		// block already seen
		synchronized (BlockChain.class) {
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

		final TimeInstant parentTimeStamp = new TimeInstant(parent.getTimestamp());

		if (block.getTimeStamp().compareTo(parentTimeStamp) < 0) {
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

		Account forgerAccount = accountAnalyzer.findByAddress(block.getSigner().getAddress());
		if (forgerAccount.getBalance().compareTo(Amount.ZERO) < 1) {
			return false;
		}

		BigInteger hit = new BigInteger(1, Arrays.copyOfRange(parent.getForgerProof(), 2, 10));
		TimeInstant blockTimeStamp = block.getTimeStamp();
		long forgerEffectiveBallance = forgerAccount.getBalance().getNumNem();
		BigInteger target = Foraging.calculateTarget(parentTimeStamp, blockTimeStamp, forgerEffectiveBallance);

		if (hit.compareTo(target) >= 0) {
			return false;
		}

		throw new RuntimeException("not yet finished");

		// 1. add block to db
		// 2. remove transactions from unconfirmed transactions.
		// run account analyzer?
	}

	public boolean addBlockToDb(Block bestBlock) {
		synchronized (BlockChain.class) {

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
