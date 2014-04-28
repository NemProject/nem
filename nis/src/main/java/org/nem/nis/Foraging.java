package org.nem.nis;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dao.TransferDao;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.mappers.BlockMapper;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.NodeApiId;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

//
// Initial logic is as follows:
//   * we receive new TX, IF it hasn't been seen,
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

	private NisPeerNetworkHost host;

	private AccountLookup accountLookup;

	private BlockChain blockChain;

	private BlockDao blockDao;

	private TransferDao transferDao;

	@Autowired
	public void setNetworkHost(final NisPeerNetworkHost host) { this.host = host; }

	@Autowired
	public void setAccountLookup(final AccountLookup accountLookup) { this.accountLookup = accountLookup; }

	@Autowired
	public void setBlockChain(final BlockChain blockChain) { this.blockChain = blockChain; }

	@Autowired
	public void setBlockDao(final BlockDao blockDao) { this.blockDao = blockDao; }

	@Autowired
	public void setTransferDao(TransferDao transferDao) { this.transferDao = transferDao; }

	public Foraging() {
		this.unlockedAccounts = new ConcurrentHashSet<>();
		this.unconfirmedTransactions = new UnconfirmedTransactions();

		this.blockGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
	}

	public void bootup() {
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
		return this.unconfirmedTransactions.add(transaction, hash -> {
			synchronized (blockChain) {
				return null != transferDao.findByHash(hash.getRaw());
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

		if (addUnconfirmedTransaction(transaction)) {
			final PeerNetwork network = this.host.getNetwork();

			// propagate transactions
			// this returns immediately, so that client who
			// actually has sent /transfer/announce won't wait for this...
			network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, transaction);

			return true;
		}
		return false;
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
		long bestScore = Long.MIN_VALUE;
		// because of access to unconfirmedTransactions, and lastBlock*

		TimeInstant blockTime = NisMain.TIME_PROVIDER.getCurrentTime();
		Collection<Transaction> transactionList = getUnconfirmedTransactionsForNewBlock(blockTime);
		final BlockScorer scorer = new BlockScorer();
		try {
			synchronized (blockChain) {
				final org.nem.nis.dbmodel.Block dbLastBlock = blockChain.getLastDbBlock();
				final Block lastBlock = BlockMapper.toModel(dbLastBlock, this.accountLookup);
				final BlockDifficulty difficulty = this.calculateDifficulty(scorer, lastBlock);

				for (Account virtualForger : unlockedAccounts) {

					// unlocked accounts are only dummies, so we need to find REAL accounts to get the balance
					final Block newBlock = createSignedBlock(blockTime, transactionList, lastBlock, virtualForger, difficulty);

					LOGGER.info("generated signature: " + HexEncoder.getString(newBlock.getSignature().getBytes()));

					final BigInteger hit = scorer.calculateHit(newBlock);
					System.out.println("   hit: 0x" + hit.toString(16));
					final BigInteger target = scorer.calculateTarget(lastBlock, newBlock);
					System.out.println("target: 0x" + target.toString(16));

					if (hit.compareTo(target) < 0) {
						System.out.println(" HIT ");

						final long score = scorer.calculateBlockScore(lastBlock, newBlock);
						if (score > bestScore) {
							bestBlock = newBlock;
							bestScore = score;
						}
					}

				}
			} // synchronized

		} catch (RuntimeException e) {
			LOGGER.warning("exception occurred during generation of a block");
			LOGGER.warning(e.toString());
		}

		if (bestBlock != null) {
			addForagedBlock(bestBlock);
		}
	}

	private BlockDifficulty calculateDifficulty(BlockScorer scorer, Block lastBlock) {
		final BlockHeight blockHeight = new BlockHeight(Math.max(1L, lastBlock.getHeight().getRaw() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION + 1));
		final List<TimeInstant> timestamps = blockDao.getTimestampsFrom(blockHeight, (int)BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION);
		final List<BlockDifficulty> difficulties = blockDao.getDifficultiesFrom(blockHeight, (int)BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION);
		return scorer.calculateDifficulty(difficulties, timestamps);
	}

	public Block createSignedBlock(
			final TimeInstant blockTime,
			final Collection<Transaction> transactionList,
			final Block lastBlock,
			final Account virtualForger,
			final BlockDifficulty difficulty) {
		final Account forger = this.accountLookup.findByAddress(virtualForger.getAddress());

		// Probably better to include difficulty in the block constructor?
		final Block newBlock = new Block(forger, lastBlock, blockTime);

		newBlock.setDifficulty(difficulty);
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
