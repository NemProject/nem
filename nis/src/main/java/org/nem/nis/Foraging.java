package org.nem.nis;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.dbmodel.Transfer;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ByteUtils;
import org.nem.core.utils.HexEncoder;
import org.nem.peer.NodeApiId;
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
public class Foraging implements AutoCloseable, Runnable {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());
	public static final long MAGIC_MULTIPLIER = 614891469L;

	private final ConcurrentHashSet<Account> unlockedAccounts;

	private final ConcurrentMap<ByteArray, Transaction> unconfirmedTransactions;

	private final ScheduledThreadPoolExecutor blockGeneratorExecutor;

	@Autowired
	private NisPeerNetworkHost host;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	@Autowired
	private BlockChain blockChain;

	private TransferDao transferDao;

	@Autowired
	public void setTransferDao(TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	public Foraging() {
		this.unlockedAccounts = new ConcurrentHashSet<>();
		this.unconfirmedTransactions = new ConcurrentHashMap<>();

		this.blockGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
		this.blockGeneratorExecutor.scheduleWithFixedDelay(this, 5, 3, TimeUnit.SECONDS);
	}

	@Override
	public void close() {
		this.blockGeneratorExecutor.shutdownNow();
	}


	public void addUnlockedAccount(Account account) {
		unlockedAccounts.add(account);
	}

	/**
	 * Adds transaction to list of unconfirmed transactions.
	 *
	 * @param transaction transaction that isValid() and verify()-ed
	 *
	 * @return false if given transaction has already been seen, true if it has been added
	 */
	public boolean addUnconfirmedTransactionWithoutDbCheck(Transaction transaction) {
		ByteArray transactionHash = new ByteArray(HashUtils.calculateHash(transaction));

		Transaction swapTest = unconfirmedTransactions.putIfAbsent(transactionHash, transaction);
		if (swapTest != null) {
			return false;
		}

		return true;
	}

	private boolean addUnconfirmedTransaction(Transaction transaction) {
		ByteArray transactionHash = new ByteArray(HashUtils.calculateHash(transaction));

		synchronized (blockChain) {
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

	/**
	 * Removes all block's transactions from list of unconfirmed transactions
	 *
	 * @param block
	 */
	public void removeFromUnconfirmedTransactions(Block block) {
		for (Transaction transaction : block.getTransactions()) {
			ByteArray transactionHash = new ByteArray(HashUtils.calculateHash(transaction));
			unconfirmedTransactions.remove(transactionHash);
		}
	}

	/**
	 * Checks if transaction fits in time limit window, if so add it to
	 * list of unconfirmed transactions.
	 *
	 * @param transaction - transaction that isValid() and verify()-ed
	 *
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

		return addUnconfirmedTransaction(transaction);
	}


	public ConcurrentMap<ByteArray, Transaction> getUnconfirmedTransactions() {
		return unconfirmedTransactions;
	}

	public List<Transaction> getUnconfirmedTransactionsForNewBlock(TimeInstant blockTIme) {
		Set<Transaction> sortedTransactions = new TreeSet<>();
		synchronized (blockChain) {
			for (Transaction tx : unconfirmedTransactions.values()) {
				if (tx.getTimeStamp().compareTo(blockTIme) < 0) {
					sortedTransactions.add(tx);
				}
			}
		}
		return new ArrayList<>(sortedTransactions);
	}

	/**
	 * Calculates "target" basing on the inputs
	 *
	 * @param parentTimeStamp         timestamp of parent block
	 * @param blockTimeStamp          timestamp or current block
	 * @param forgerEffectiveBallance - effective balance used to forage
	 *
	 * @return The target.
	 */
	public static BigInteger calculateTarget(TimeInstant parentTimeStamp, TimeInstant blockTimeStamp, long forgerEffectiveBallance) {
		return BigInteger.valueOf(blockTimeStamp.subtract(parentTimeStamp)).multiply(
				BigInteger.valueOf(forgerEffectiveBallance).multiply(
						BigInteger.valueOf(MAGIC_MULTIPLIER)
				)
		);
	}

	/**
	 * Calculates how "good" given block is.
	 *
	 * @param block
	 *
	 * @return score of a block.
	 */
	private long calcBlockScore(Block block) {
		long r1 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(block.getSignature().getBytes(), 10, 14)));
		long r2 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(HashUtils.calculateHash(block), 10, 14)));

		return r1 + r2;
	}

	@Override
	public void run() {
		if (blockChain.getLastDbBlock() == null) {
			return;
		}

		LOGGER.info("block generation " + Integer.toString(unconfirmedTransactions.size()) + " " + Integer.toString(unlockedAccounts.size()));

		Block bestBlock = null;
		long bestScore = Long.MAX_VALUE;
		// because of access to unconfirmedTransactions, and lastBlock*

		TimeInstant blockTime = NisMain.TIME_PROVIDER.getCurrentTime();
		List<Transaction> transactionList = getUnconfirmedTransactionsForNewBlock(blockTime);
		synchronized (blockChain) {
			for (Account forger : unlockedAccounts) {
				Block newBlock = new Block(forger, blockChain.getLastBlockHash(), blockTime, blockChain.getLastBlockHeight() + 1);
				if (transactionList.size() > 0) {
					newBlock.addTransactions(transactionList);
				}

				newBlock.sign();

				LOGGER.info("generated signature: " + HexEncoder.getString(newBlock.getSignature().getBytes()));

				// dummy forging rule

				// unlocked accounts are only dummies, so we need to find REAL accounts to get the balance
				Account realAccout = accountAnalyzer.findByAddress(forger.getAddress());
				if (realAccout.getBalance().compareTo(Amount.ZERO) < 1) {
					continue;
				}

				BigInteger hit = new BigInteger(1, Arrays.copyOfRange(blockChain.getLastBlockSignature(), 2, 10));
				long effectiveBalance = realAccout.getBalance().getNumNem();
				BigInteger target = calculateTarget(new TimeInstant(blockChain.getLastBlockTimestamp()), newBlock.getTimeStamp(), effectiveBalance);

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
			addForagedBlock(bestBlock);
		}
	}

	private void addForagedBlock(Block bestBlock) {
		//
		// if we're here it means unconfirmed transactions haven't been
		// seen in any block yet, so we can add this block to local db
		//
		// (if at some point later we receive better block,
		// fork resolution will handle that)
		//
		if (blockChain.addBlockToDb(bestBlock)) {
			removeFromUnconfirmedTransactions(bestBlock);

			// TODO: should this be called by Foraging? or maybe somewhere in blockchain
			host.getNetwork().broadcast(NodeApiId.REST_PUSH_BLOCK, bestBlock);
		}
	}
}
