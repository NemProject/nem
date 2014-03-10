package org.nem.nis;


import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.model.*;

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

	public BlockChain() {
		this.unconfirmedTransactions = new ConcurrentHashMap<>();

		this.blockGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
		this.blockGeneratorExecutor.scheduleWithFixedDelay(new BlockGenerator(), 10, 10, TimeUnit.SECONDS);

		this.unlockedAccounts = new ConcurrentHashSet<>();
	}

	public void bootup() {
		LOGGER.info("booting up block generator");
	}
	
	/**
	 *
	 * @param transaction - transaction that isValid() and verify()-ed
	 * @return
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


			LOGGER.info("block generation " + Integer.toString(unconfirmedTransactions.size()) + " " + Integer.toString(unlockedAccounts.size()));
		}
	}
}
