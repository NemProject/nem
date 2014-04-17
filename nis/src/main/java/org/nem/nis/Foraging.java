package org.nem.nis;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.utils.Predicate;
import org.nem.nis.dao.TransferDao;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.mappers.BlockMapper;
import org.nem.peer.NodeApiId;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.*;
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

	private final ConcurrentHashSet<Account> unlockedAccounts;

	private final UnconfirmedTransactions unconfirmedTransactions;

	private final ScheduledThreadPoolExecutor blockGeneratorExecutor;

	@Autowired
	private NisPeerNetworkHost host;

	private AccountAnalyzer accountAnalyzer;

	private BlockChain blockChain;

	private TransferDao transferDao;

	@Autowired
	public void setAccountAnalyzer(AccountAnalyzer accountAnalyzer) { this.accountAnalyzer = accountAnalyzer; }

	@Autowired
	public void setBlockChain(BlockChain blockChain) { this.blockChain = blockChain; }

	@Autowired
	public void setTransferDao(TransferDao transferDao) { this.transferDao = transferDao; }

	public Foraging() {
		this.unlockedAccounts = new ConcurrentHashSet<>();
		this.unconfirmedTransactions = new UnconfirmedTransactions();

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
	 * Gets the number of unconfirmed transactions.
	 *
	 * @return The number of unconfirmed transactions.
	 */
	public int getNumUnconfirmedTransactions() {
		return this.unconfirmedTransactions.size();
	}

	/**
	 * Adds transaction to list of unconfirmed transactions.
	 *
	 * @param transaction transaction that isValid() and verify()-ed
	 *
	 * @return false if given transaction has already been seen, true if it has been added
	 */
	public boolean addUnconfirmedTransactionWithoutDbCheck(Transaction transaction) {
		return this.unconfirmedTransactions.add(transaction);
	}

	private boolean addUnconfirmedTransaction(Transaction transaction) {
		return this.unconfirmedTransactions.add(transaction, new Predicate<Hash>() {
			@Override
			public boolean evaluate(final Hash hash) {
				synchronized (blockChain) {
					return null != transferDao.findByHash(hash.getRaw());
				}
			}
		});
	}

	/**
	 * Removes all block's transactions from list of unconfirmed transactions
	 *
	 * @param block
	 */
	public void removeFromUnconfirmedTransactions(Block block) {
		this.unconfirmedTransactions.removeAll(block);
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

	public List<Transaction> getUnconfirmedTransactionsForNewBlock(TimeInstant blockTime) {
		return this.unconfirmedTransactions.getTransactionsBefore(blockTime);
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
		Collection<Transaction> transactionList = getUnconfirmedTransactionsForNewBlock(blockTime);
		final BlockScorer scorer = new BlockScorer();
		try {
			synchronized (blockChain) {
				final org.nem.nis.dbmodel.Block dbLastBlock = blockChain.getLastDbBlock();
				final Block lastBlock = BlockMapper.toModel(dbLastBlock, this.accountAnalyzer);
				final BigInteger hit = scorer.calculateHit(lastBlock);
				System.out.println("   hit: 0x" + hit.toString(16));

				for (Account virtualForger : unlockedAccounts) {

					// unlocked accounts are only dummies, so we need to find REAL accounts to get the balance
					final Block newBlock = createSignedBlock(blockTime, transactionList, lastBlock, virtualForger);

					LOGGER.info("generated signature: " + HexEncoder.getString(newBlock.getSignature().getBytes()));

					final BigInteger target = scorer.calculateTarget(lastBlock, newBlock);
					System.out.println("target: 0x" + target.toString(16));

					if (hit.compareTo(target) < 0) {
						System.out.println(" HIT ");

						final long score = scorer.calculateBlockScore(lastBlock, newBlock);
						if (score < bestScore) {
							bestBlock = newBlock;
							bestScore = score;
						}
					}

				}
			} // synchronized

		} catch (RuntimeException e) {
			LOGGER.warning("exception occured during generation of a block");
			LOGGER.warning(e.toString());
		}

		if (bestBlock != null) {
			addForagedBlock(bestBlock);
		}
	}

	public Block createSignedBlock(TimeInstant blockTime, Collection<Transaction> transactionList, Block lastBlock, Account virtualForger) {
		final Account forger = accountAnalyzer.findByAddress(virtualForger.getAddress());

		final Block newBlock = new Block(forger, lastBlock, blockTime);
		if (!transactionList.isEmpty()) {
			newBlock.addTransactions(transactionList);
		}

		newBlock.signBy(virtualForger);
		return newBlock;
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
