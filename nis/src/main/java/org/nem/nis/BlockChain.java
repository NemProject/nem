package org.nem.nis;


import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.dbmodel.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.utils.ArrayUtils;
import org.nem.core.utils.HexEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BlockChain {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	private ConcurrentMap<ByteArray, Transaction> unconfirmedTransactions;
	private final ScheduledThreadPoolExecutor blockGeneratorExecutor;

	// this should be somewhere else
	private ConcurrentHashSet<Account> unlockedAccounts;

	// for now it's easier to keep it like this
	private byte[] lastBlockHash;
	private Long lastBlockHeight;
	private byte[] lastBlockSignature;
	private int lastBlockTimestamp;


	public BlockChain() {
		this.unconfirmedTransactions = new ConcurrentHashMap<>();

		this.blockGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
		this.blockGeneratorExecutor.scheduleWithFixedDelay(new BlockGenerator(), 10, 10, TimeUnit.SECONDS);

		this.unlockedAccounts = new ConcurrentHashSet<>();
	}

	public void bootup() {
		LOGGER.info("booting up block generator");
	}


	public void analyzeLastBlock(org.nem.core.dbmodel.Block curBlock) {
		LOGGER.info("analyzing last block: " + Long.toString(curBlock.getShortId()));

		lastBlockHash = ArrayUtils.duplicate(curBlock.getBlockHash());
		lastBlockHeight = curBlock.getHeight();
		lastBlockSignature = ArrayUtils.duplicate(curBlock.getForgerProof());
		lastBlockTimestamp = curBlock.getTimestamp();
	}

	/**
	 *
	 * @param transaction - transaction that isValid() and verify()-ed
	 * @return false if given transaction has already been seen, true if it has been added
	 */
	public boolean processTransaction(Transaction transaction) {

		int currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		// rest is checked by isValid()
		if (transaction.getTimestamp() > currentTime + 30) {
			return false;
		}

		ByteArray transactionHash = new ByteArray(HashUtils.calculateHash(transaction));

		synchronized (BlockChain.class) {
			// TODO: check if transaction isn't already in DB
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

	public byte[] getLastBlockHash() {
		return lastBlockHash;
	}

	public Long getLastBlockHeight() {
		return lastBlockHeight;
	}

	public byte[] getLastBlockSignature() {
		return lastBlockSignature;
	}

	public ConcurrentMap<ByteArray, Transaction> getUnconfirmedTransactions() {
		return unconfirmedTransactions;
	}

	class BlockGenerator implements Runnable {

		@Override
		public void run() {
			if (lastBlockHash == null) {
				return;
			}

			if (unconfirmedTransactions.size() == 0) {
				return;
			}

			LOGGER.info("block generation " + Integer.toString(unconfirmedTransactions.size()) + " " + Integer.toString(unlockedAccounts.size()));

			List<Transaction> transactionList;
			long totalFee = 0;

			// because of access to unconfirmedTransactions, and lastBlock*
			synchronized (BlockChain.class) {
				// first prepare
				Set<Transaction> sortedTransactions = new HashSet<>(unconfirmedTransactions.values());
				LOGGER.warning("hello: " + Integer.toString(sortedTransactions.size()));
				transactionList = new ArrayList<>(sortedTransactions);
				for (Transaction transaction : transactionList) {
					totalFee += transaction.getFee();
				}

				boolean forged = false;
				for (Account forger : unlockedAccounts) {
					Block newBlock = new Block(forger, lastBlockHash, NisMain.TIME_PROVIDER.getCurrentTime(), lastBlockHeight + 1);
					newBlock.setTransactions(transactionList, totalFee);

					newBlock.sign();

					LOGGER.info("generated signature: " + HexEncoder.getString(newBlock.getSignature().getBytes()));

					// dummy forging rule

					// unlocked accounts are only dummies, so we need to find REAL accounts to get the balance
					Account realAccout = accountAnalyzer.findByAddress(forger.getAddress());
					if (realAccout.getBalance() < 1) {
						continue;
					}

					BigInteger hit = new BigInteger(1, Arrays.copyOfRange(lastBlockSignature, 2, 10));
					BigInteger target = BigInteger.valueOf(newBlock.getTimeStamp() - lastBlockTimestamp).multiply(
							BigInteger.valueOf(realAccout.getBalance()).multiply(
									BigInteger.valueOf(30745)
							)
					);

					System.out.println("hit: " + hit.toString());
					System.out.println("hit: 0x" + hit.toString(16));
					System.out.println("hit: 0x" + target.toString(16));
					System.out.println("hit: " + target.toString());

					if (hit.compareTo(target) < 0) {
						System.out.println(" HIT ");
						//forged = true;
					}

				}

				if (forged) {
					unconfirmedTransactions.clear();
				}
			}
		}
	}
}
