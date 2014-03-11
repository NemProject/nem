package org.nem.nis;


import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.dbmodel.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.time.TimeProvider;
import org.nem.core.utils.HexEncoder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BlockChain {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());
	public static BlockChain MAIN_CHAIN = new BlockChain();

	private ConcurrentMap<ByteArray, Transaction> unconfirmedTransactions;
	private final ScheduledThreadPoolExecutor blockGeneratorExecutor;

	// this should be somewhere else
	private ConcurrentHashSet<Account> unlockedAccounts;

	//
	private byte[] lastBlockHash;
	private Long lastBlockHeight;


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

		lastBlockHash = curBlock.getBlockHash();
		lastBlockHeight = curBlock.getHeight();
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

	class BlockGenerator implements Runnable {

		@Override
		public void run() {
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

					// TODO: add some dummy forging rule

					// forged = true;
				}

				if (forged) {
					unconfirmedTransactions.clear();
				}
			}
		}
	}
}
