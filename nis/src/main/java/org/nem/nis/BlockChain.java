package org.nem.nis;


import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.mappers.AccountDaoLookupAdapter;
import org.nem.core.mappers.BlockMapper;
import org.nem.core.dao.AccountDao;
import org.nem.core.dao.BlockDao;
import org.nem.core.dao.TransferDao;
import org.nem.core.dbmodel.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.time.TimeInstant;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.ByteUtils;
import org.nem.core.utils.HexEncoder;
import org.nem.peer.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

//
// Initial logic is as follows:
//   * we recieve new TX, IF it hasn't been seen,
//     it is added to unconfirmedTransactions,
//   * blockGeneratorExecutor periodically tries to generate a block containing
//     unconfirmed transactions
//   * if it succeeded, block is added to the db and propagated to the network
//
// fork resolution should solve the rest
//
public class BlockChain implements BlockSynchronizer {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());
	// 500_000_000 nems have force to generate block every minute
	public static final long MAGIC_MULTIPLIER = 614891469L;
	//
	public static final long ESTIMATED_BLOCKS_PER_DAY = 1440;

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

    @Autowired
    private NisPeerNetworkHost host;

	// for now it's easier to keep it like this
	org.nem.core.dbmodel.Block lastBlock;

	public BlockChain() {
	}

	public void bootup() {
		LOGGER.info("booting up block generator");
	}

	public org.nem.core.dbmodel.Block getLastDbBlock() {
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


	private long calcDbBlockScore(org.nem.core.dbmodel.Block block) {
		long r1 = Math.abs((long) ByteUtils.bytesToInt(Arrays.copyOfRange(block.getForgerProof(), 10, 14)));
		long r2 = Math.abs((long) ByteUtils.bytesToInt(Arrays.copyOfRange(block.getBlockHash(), 10, 14)));

		return r1 + r2;
	}

	private long calcBlockScore(Block block) {
		long r1 = Math.abs((long) ByteUtils.bytesToInt(Arrays.copyOfRange(block.getSignature().getBytes(), 10, 14)));
		long r2 = Math.abs((long) ByteUtils.bytesToInt(Arrays.copyOfRange(HashUtils.calculateHash(block), 10, 14)));

		return r1 + r2;
	}

	public void analyzeLastBlock(org.nem.core.dbmodel.Block curBlock) {
		LOGGER.info("analyzing last block: " + Long.toString(curBlock.getShortId()));
		lastBlock = curBlock;

		Block block = BlockMapper.toModel(lastBlock, accountAnalyzer);
	}


	public boolean synchronizeCompareBlocks(Block peerLastBlock, org.nem.core.dbmodel.Block dbBlock) {
		if (peerLastBlock.getHeight() == dbBlock.getHeight()) {
			if (Arrays.equals(HashUtils.calculateHash(peerLastBlock), dbBlock.getBlockHash())) {
				if (Arrays.equals(peerLastBlock.getSignature().getBytes(), dbBlock.getForgerProof())) {
					return true;
				}
			}
		}
		return false;
	}

	public enum SynchronizeCompareStatus {
		EVIL_NODE,
		EQUAL_BLOCKS,
		OUR_CHAIN_IS_BETTER,
		PEER_CHAIN_IS_BETTER,
	}

	public SynchronizeCompareStatus sychronizeCompareAt(Block peerBlock, long lowerHeight) {
		if (! peerBlock.verify()) {
			// TODO: penalty for node
			return SynchronizeCompareStatus.EVIL_NODE;
		}

		org.nem.core.dbmodel.Block ourBlock = blockDao.findByHeight(lowerHeight);

		if (synchronizeCompareBlocks(peerBlock, ourBlock)) {
			return SynchronizeCompareStatus.EQUAL_BLOCKS;
		}

		long peerScore = calcBlockScore(peerBlock);
		long ourScore = calcDbBlockScore(ourBlock);
		if (peerScore < ourScore) {
			return SynchronizeCompareStatus.PEER_CHAIN_IS_BETTER;
		}

		return SynchronizeCompareStatus.OUR_CHAIN_IS_BETTER;
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

	@Override
	public void synchronizeNode(PeerConnector connector, Node node) {
		Block peerLastBlock = connector.getLastBlock(node.getEndpoint());
		if (peerLastBlock == null) {
			return;
		}

		if (this.synchronizeCompareBlocks(peerLastBlock, lastBlock)) {
			return;
		}

		long val = peerLastBlock.getHeight() - this.getLastBlockHeight();
		long lowerHeight = Math.min(peerLastBlock.getHeight(), this.getLastBlockHeight());

		// if node is far behind, reject it, not to allow too deep
		// rewrites of blockchain...
		if (val < -(ESTIMATED_BLOCKS_PER_DAY / 2)) {
			return;
		}

		Block commonBlock = peerLastBlock;
		if (val > 0) {
			commonBlock = connector.getBlockAt(node.getEndpoint(), lowerHeight);
			// no point to continue
			if (commonBlock == null) {
				return;
			}
		}

		SynchronizeCompareStatus status = this.sychronizeCompareAt(commonBlock, lowerHeight);

		if (status == SynchronizeCompareStatus.PEER_CHAIN_IS_BETTER) {
			// TODO: find common block
			// remember to check it height diff < halfday
			// update val
			// update commonBlock
			LOGGER.severe("finding common block not handled yet");
			System.exit(-1);
		}

		switch (status) {
			case EVIL_NODE:
				// TODO: PENALTY for node
				return;
			case OUR_CHAIN_IS_BETTER:
				// perfect nothing to do
				return;
			default:
				break;
		}

		if (status == SynchronizeCompareStatus.EQUAL_BLOCKS) {
			long peerHeight = commonBlock.getHeight();

			// if 'common' block is peer's last one it simply means we have longer chain
			if (peerHeight == peerLastBlock.getHeight()) {
				return;
			}

			org.nem.core.dbmodel.Block ourDbBlock = blockDao.findByHeight(peerHeight);
			if (ourDbBlock == null) {
				// probably would be strange if that would happen
				return;
			}

			AccountAnalyzer contemporaryAccountAnalyzer = new AccountAnalyzer(accountAnalyzer);
			if (this.getLastBlockHeight() > peerHeight) {
				// TODO: create duplicate of account analyzer
				// revert transactions "on the copy"
				LOGGER.severe("virtual chain not handled yet");
				System.exit(-1);
			}

			List<Block> peerChain = connector.getChainAfter(node.getEndpoint(), peerHeight);
			if (peerChain.size() > (ESTIMATED_BLOCKS_PER_DAY/2)) {
				// TODO: PENALTY for node
				return;
			}

			// do not trust peer, take block from our db and convert it
			Block parentBlock = BlockMapper.toModel(ourDbBlock, contemporaryAccountAnalyzer);

			long wantedHeight = peerHeight + 1;
			for (Block block : peerChain) {
				if (block.getHeight() != wantedHeight ||
						! block.verify() ||
						! validateBlock(block, parentBlock, contemporaryAccountAnalyzer)) {
					// TODO: PENALTY for node
					return;
				}

				for (Transaction transaction : block.getTransactions()) {
					if (! transaction.isValid()) {
						// TODO: PENALTY for node
						return;
					}
					if (! transaction.verify()) {
						// TODO: PENALTY for node
						return;
					}
				}

				parentBlock = block;

				wantedHeight += 1;
			}
		}
	}


	/**
	 * Checks if passed block is correct, and if eligible adds it to db
	 *
	 * @param block - block that's going to be processed
	 * @return false if block was known or invalid, true if ok and added to db
	 */
	public boolean processBlock(Block block) {
		byte[] blockHash = HashUtils.calculateHash(block);
		byte[] parentHash = block.getPreviousBlockHash();

		org.nem.core.dbmodel.Block parent;

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
			org.nem.core.dbmodel.Block child = blockDao.findById(parent.getNextBlockId());
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

			final org.nem.core.dbmodel.Block dbBlock = BlockMapper.toDbModel(bestBlock, new AccountDaoLookupAdapter(this.accountDao));

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
