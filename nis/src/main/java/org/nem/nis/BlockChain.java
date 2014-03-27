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
public class BlockChain implements AutoCloseable, BlockSynchronizer {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());
	// 500_000_000 nems have force to generate block every minute
	public static final long MAGIC_MULTIPLIER = 614891469L;
	//
	public static final long ESTIMATED_BLOCKS_PER_DAY = 1440;

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	private TransferDao transferDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

    @Autowired
    private NisPeerNetworkHost host;

	private final ConcurrentMap<ByteArray, Transaction> unconfirmedTransactions;
	private final ScheduledThreadPoolExecutor blockGeneratorExecutor;

	// this should be somewhere else
	private final ConcurrentHashSet<Account> unlockedAccounts;

	// for now it's easier to keep it like this
	org.nem.core.dbmodel.Block lastBlock;

	public BlockChain() {
		this.unconfirmedTransactions = new ConcurrentHashMap<>();

		this.blockGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
		this.blockGeneratorExecutor.scheduleWithFixedDelay(new BlockGenerator(), 5, 3, TimeUnit.SECONDS);

		this.unlockedAccounts = new ConcurrentHashSet<>();
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

	public long getLastBlockScore() {
		return calcDbBlockScore(lastBlock);
	}

	public ConcurrentMap<ByteArray, Transaction> getUnconfirmedTransactions() {
		return unconfirmedTransactions;
	}

	@Override
	public void close() {
		this.blockGeneratorExecutor.shutdownNow();
	}

	@Autowired
	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
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
			// no point no continue
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
				// TODO: penalty for a node
				return;
			case OUR_CHAIN_IS_BETTER:
				// perfect nothing to do
				return;
			default:
				break;
		}

		if (status == SynchronizeCompareStatus.EQUAL_BLOCKS) {
			long peerHeight = commonBlock.getHeight();

			if (this.getLastBlockHeight() > peerHeight) {
				// TODO: create duplicate of account analyzer
				// revert transactions "on the copy"
				LOGGER.severe("virtual chain not handled yet");
				System.exit(-1);
			}

			List<Block> peerChain = connector.getChainAfter(node.getEndpoint(), peerHeight);
		}
	}

	/**
	 * Calculates "target" basing on the inputs
	 *
	 * @param parentTimeStamp timestamp of parent block
	 * @param blockTimeStamp timestamp or current block
	 * @param forgerEffectiveBallance - effective balance used to forage
	 *
	 * @return The target.
	 */
	private BigInteger calculateTarget(TimeInstant parentTimeStamp, TimeInstant blockTimeStamp, long forgerEffectiveBallance) {
		return BigInteger.valueOf(blockTimeStamp.subtract(parentTimeStamp)).multiply(
				BigInteger.valueOf(forgerEffectiveBallance).multiply(
						BigInteger.valueOf(MAGIC_MULTIPLIER)
				)
		);
	}

	/**
	 *
	 * @param transaction - transaction that isValid() and verify()-ed
	 * @return false if given transaction has already been seen, true if it has been added
	 */
	public boolean processTransaction(Transaction transaction) {

		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		// rest is checked by isValid()
		if (transaction.getTimeStamp().compareTo(currentTime.addSeconds(30)) > 0) {
			return false;
		}
		if (transaction.getTimeStamp().compareTo(currentTime.addSeconds(-30)) < 0) {
			return false;
		}

		ByteArray transactionHash = new ByteArray(HashUtils.calculateHash(transaction));

		synchronized (BlockChain.class) {
			Transfer tx = transferDao.findByHash(transactionHash.get());
			if (tx != null) {
				return false;
			}
		}

		Transaction swapTest = unconfirmedTransactions.putIfAbsent(transactionHash, transaction);
		if (swapTest != null) {
			return false;
		}

		return true;
	}

	public void addUnlockedAccount(Account account) {
		unlockedAccounts.add(account);
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

		final TimeInstant parentTimeStamp = new TimeInstant(parent.getTimestamp());

		// if we don't have parent, we can't do anything with this block
		if (parent == null) {
			return false;
		}

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
		if (Amount.ZERO.compareTo(forgerAccount.getBalance()) < 1) {
			return false;
		}

		BigInteger hit = new BigInteger(1, Arrays.copyOfRange(parent.getForgerProof(), 2, 10));
		TimeInstant blockTimeStamp = block.getTimeStamp();
		long forgerEffectiveBallance = forgerAccount.getBalance().getNumNem();
		BigInteger target = calculateTarget(parentTimeStamp, blockTimeStamp, forgerEffectiveBallance);

		if (hit.compareTo(target) >= 0) {
			return false;
		}

		throw new RuntimeException("not yet finished");

		// 1. add block to db
		// 2. remove transactions from unconfirmed transactions.
		// run account analyzer?
	}

	// not sure where it should be
	private boolean addBlockToDb(Block bestBlock) {
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


	public List<Transaction> getUnconfirmedTransactionsForNewBlock(TimeInstant blockTIme) {
		Set<Transaction> sortedTransactions = new TreeSet<>();
		synchronized (BlockChain.class) {
			for (Transaction tx : unconfirmedTransactions.values()) {
				if (tx.getTimeStamp().compareTo(blockTIme) < 0) {
					sortedTransactions.add(tx);
				}
			}
		}
		return new ArrayList<>(sortedTransactions);
	}

	class BlockGenerator implements Runnable {

		@Override
		public void run() {
			if (lastBlock == null) {
				return;
			}

			LOGGER.info("block generation " + Integer.toString(unconfirmedTransactions.size()) + " " + Integer.toString(unlockedAccounts.size()));

			Block bestBlock = null;
			long bestScore = Long.MAX_VALUE;
			// because of access to unconfirmedTransactions, and lastBlock*

			TimeInstant blockTime = NisMain.TIME_PROVIDER.getCurrentTime();
			List<Transaction> transactionList = getUnconfirmedTransactionsForNewBlock(blockTime);
			synchronized (BlockChain.class) {
				for (Account forger : unlockedAccounts) {
					Block newBlock = new Block(forger, lastBlock.getBlockHash(), blockTime, lastBlock.getHeight() + 1);
					newBlock.addTransactions(transactionList);

					newBlock.sign();

					LOGGER.info("generated signature: " + HexEncoder.getString(newBlock.getSignature().getBytes()));

					// dummy forging rule

					// unlocked accounts are only dummies, so we need to find REAL accounts to get the balance
					Account realAccout = accountAnalyzer.findByAddress(forger.getAddress());
					if (realAccout.getBalance().compareTo(Amount.ZERO) < 1) {
						continue;
					}

					BigInteger hit = new BigInteger(1, Arrays.copyOfRange(lastBlock.getForgerProof(), 2, 10));
					long effectiveBalance = realAccout.getBalance().getNumNem();
					BigInteger target = calculateTarget(new TimeInstant(lastBlock.getTimestamp()), newBlock.getTimeStamp(), effectiveBalance);

					System.out.println("   hit: 0x" + hit.toString(16));
					System.out.println("target: 0x" + target.toString(16));

					if (hit.compareTo(target) < 0) {
						System.out.println(" HIT ");


						long score = calcBlockScore(newBlock);
						if (score < bestScore) {
							bestBlock = newBlock;
							bestScore = score;
						}
					}

				}
			} // synchronized

			if (bestBlock != null) {
				//
				// if we're here it means unconfirmed transactions haven't been
				// seen in any block yet, so we can add this block to local db
				//
				// (if at some point later we receive better block,
				// fork resolution will handle that)
				//
				if (addBlockToDb(bestBlock)) {
					for (Transaction transaction : bestBlock.getTransactions()) {
						ByteArray transactionHash = new ByteArray(HashUtils.calculateHash(transaction));
						unconfirmedTransactions.remove(transactionHash);
					}

					host.getNetwork().broadcast(NodeApiId.REST_PUSH_BLOCK, bestBlock);
				}
			}
		}
	}
}
