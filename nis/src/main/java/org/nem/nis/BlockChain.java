package org.nem.nis;


import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.dao.AccountDao;
import org.nem.core.dao.BlockDao;
import org.nem.core.dao.TransferDao;
import org.nem.core.dbmodel.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.ArrayUtils;
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

	@Autowired
	private TransferDao transactionDao;

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

		int currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		// rest is checked by isValid()
		if (transaction.getTimeStamp() > currentTime + 30) {
			return false;
		}

		ByteArray transactionHash = new ByteArray(HashUtils.calculateHash(transaction));

		synchronized (BlockChain.class) {
			Transfer tx = transactionDao.findByHash(transactionHash.get());
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

	public boolean processBlock(Block block) {
		byte[] blockHash = HashUtils.calculateHash(block);
		org.nem.core.dbmodel.Block b = blockDao.findByHash(blockHash);
		throw new RuntimeException("not yet done");
	}


	// not sure where it should be
	private boolean addBlockToDb(Block bestBlock) {
		synchronized (BlockChain.class) {
			org.nem.core.dbmodel.Account forager = accountDao.getAccountByPrintableAddress(bestBlock.getSigner().getAddress().getEncoded());

			byte[] blockHash = HashUtils.calculateHash(bestBlock);
			org.nem.core.dbmodel.Block dbBlock = new org.nem.core.dbmodel.Block(
					ByteUtils.bytesToLong(blockHash),
					bestBlock.getVersion(),
					bestBlock.getPreviousBlockHash(),
					blockHash,
					bestBlock.getTimeStamp(),
					forager,
					bestBlock.getSignature().getBytes(),
					bestBlock.getHeight(),
					0L,
					bestBlock.getTotalFee()
			);

			int i = 0;
			List<Transfer> transactions = new ArrayList<>(bestBlock.getTransactions().size());

			for (Transaction transaction : bestBlock.getTransactions()) {
				final TransferTransaction transferTransaction = (TransferTransaction)transaction;
				org.nem.core.dbmodel.Account sender = accountDao.getAccountByPrintableAddress(transaction.getSigner().getAddress().getEncoded());
				org.nem.core.dbmodel.Account recipient = accountDao.getAccountByPrintableAddress(transferTransaction.getRecipient().getAddress().getEncoded());
				byte[] txHash = HashUtils.calculateHash(transferTransaction);
				Transfer dbTransfer = new Transfer(
						ByteUtils.bytesToLong(txHash),
						txHash,
						transferTransaction.getVersion(),
						transferTransaction.getType(),
						transferTransaction.getFee(),
						transferTransaction.getTimeStamp(),
						transferTransaction.getDeadline(),
						sender,
						// proof
						transferTransaction.getSignature().getBytes(),
						recipient,
						i, // index
						transferTransaction.getAmount(),
						0L // referenced tx
				);
				dbTransfer.setBlock(dbBlock);
				transactions.add(dbTransfer);
				i++;
			}

			dbBlock.setBlockTransfers(transactions);

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

			List<Transaction> transactionList;
			Block bestBlock = null;
			long bestScore = Long.MAX_VALUE;
			// because of access to unconfirmedTransactions, and lastBlock*
			synchronized (BlockChain.class) {
				//
				// TODO: the following code mut be changed to include only TXes, that have deadline < current time
				//
				Set<Transaction> sortedTransactions = new HashSet<>(unconfirmedTransactions.values());
				transactionList = new ArrayList<>(sortedTransactions);

				for (Account forger : unlockedAccounts) {
					Block newBlock = new Block(forger, lastBlock.getBlockHash(), NisMain.TIME_PROVIDER.getCurrentTime(), lastBlock.getHeight() + 1);
					newBlock.addTransactions(transactionList);

					newBlock.sign();

					LOGGER.info("generated signature: " + HexEncoder.getString(newBlock.getSignature().getBytes()));

					// dummy forging rule

					// unlocked accounts are only dummies, so we need to find REAL accounts to get the balance
					Account realAccout = accountAnalyzer.findByAddress(forger.getAddress());
					if (realAccout.getBalance() < 1) {
						continue;
					}

					BigInteger hit = new BigInteger(1, Arrays.copyOfRange(lastBlock.getForgerProof(), 2, 10));
					BigInteger target = BigInteger.valueOf(newBlock.getTimeStamp() - lastBlock.getTimestamp()).multiply(
							BigInteger.valueOf(realAccout.getBalance()).multiply(
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
