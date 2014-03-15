package org.nem.nis;


import org.eclipse.jetty.util.ConcurrentHashSet;
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
import org.nem.peer.NodeApiId;
import org.nem.peer.PeerNetworkHost;
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
public class BlockChain {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	private TransferDao transferDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	private ConcurrentMap<ByteArray, Transaction> unconfirmedTransactions;
	private final ScheduledThreadPoolExecutor blockGeneratorExecutor;

	// this should be somewhere else
	private ConcurrentHashSet<Account> unlockedAccounts;

	// for now it's easier to keep it like this
	org.nem.core.dbmodel.Block lastBlock;

	public BlockChain() {
		this.unconfirmedTransactions = new ConcurrentHashMap<>();

		this.blockGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
		this.blockGeneratorExecutor.scheduleWithFixedDelay(new BlockGenerator(), 10, 10, TimeUnit.SECONDS);

		this.unlockedAccounts = new ConcurrentHashSet<>();
	}

	public void bootup() {
		LOGGER.info("booting up block generator");
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

	public ConcurrentMap<ByteArray, Transaction> getUnconfirmedTransactions() {
		return unconfirmedTransactions;
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

		// block already seen
		if (blockDao.findByHash(blockHash) != null) {
			return false;
		}

		// check if we know previous block
		byte[] parentHash = block.getPreviousBlockHash();
		org.nem.core.dbmodel.Block parent = blockDao.findByHash(parentHash);
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
		BigInteger target = BigInteger.valueOf(block.getTimeStamp().subtract(parentTimeStamp)).multiply(
                BigInteger.valueOf(forgerAccount.getBalance().getNumMicroNem()).multiply(
                        BigInteger.valueOf(30745)
                )
        );

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

            final org.nem.core.dbmodel.Block dbBlock = BlockMapper.toDbModel(bestBlock, this.accountDao);

			// hibernate will save both block AND transactions
			// as there is cascade in Block
			// mind that there is NO cascade in transaction
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

			if (unconfirmedTransactions.size() == 0) {
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
					BigInteger target = BigInteger.valueOf(newBlock.getTimeStamp().subtract(new TimeInstant(lastBlock.getTimestamp()))).multiply(
                            BigInteger.valueOf(realAccout.getBalance().getNumMicroNem()).multiply(
                                    BigInteger.valueOf(30745)
                            )
                    );

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
					unconfirmedTransactions.clear();

					PeerNetworkHost peerNetworkHost = PeerNetworkHost.getDefaultHost();
					peerNetworkHost.getNetwork().broadcast(NodeApiId.REST_PUSH_BLOCK, bestBlock);
				}
			}
		}
	}
}
