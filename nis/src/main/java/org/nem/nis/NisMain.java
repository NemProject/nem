package org.nem.nis;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Vector;

import javax.annotation.PostConstruct;

import java.util.logging.Logger;

import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.Hashes;
import org.nem.core.dao.AccountDao;
import org.nem.core.dao.BlockDao;

import org.nem.core.dao.TransferDao;
import org.nem.core.model.*;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.ByteUtils;
import org.nem.core.utils.HexEncoder;
import org.nem.core.utils.StringEncoder;
import org.nem.core.dbmodel.Transfer;
import org.nem.peer.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

public class NisMain {
	private static final Logger logger = Logger.getLogger(NisMain.class.getName());

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private TransferDao transactionDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	BlockAnalyzer blockAnalyzer;

	public NisMain() {
	}

	static long epochBeginning = NisMain.initEpoch();

	static private long initEpoch() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.set(Calendar.YEAR, 2014);
		calendar.set(Calendar.MONTH, Calendar.MARCH);
		calendar.set(Calendar.DAY_OF_MONTH, 01);
		calendar.set(Calendar.HOUR, 12);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis();
	}

	public static int getEpochTime() {
		return (int) ((System.currentTimeMillis() - epochBeginning + 500L) / 1000L);
	}

	private void analyzeBlocks() {
		Long curBlockId = Genesis.BLOCK_ID;
		org.nem.core.dbmodel.Block curBlock;
		System.out.println("starting analysis...");
		while ((curBlock = blockDao.findByShortId(curBlockId)) != null) {
			blockAnalyzer.analyze(curBlock);
			accountAnalyzer.analyze(curBlock);

			curBlockId = curBlock.getNextBlockId();
			if (curBlockId == null) {
				break;
			}
		}
	}

	@PostConstruct
	private void init() {
		logger.warning("context ================== epoch: " + Long.toHexString(epochBeginning) + " current: " + Long.toHexString(getEpochTime()));

		/** 
		 * Thies1965, something is still wrong with my set-up
		 * I get an SQL exception
		 * Just for my testing purposes, I commented the next lines out
		 */
		
		populateDb();

		blockAnalyzer = new BlockAnalyzer();

		analyzeBlocks();

		PeerNetworkHost peerNetworkHost = PeerNetworkHost.getDefaultHost();
	}

	private void populateDb() {
		Account genesisAccount = getGenesisAccount();
		org.nem.core.dbmodel.Account dbGenesisAccount = populateGenesisAccount(genesisAccount);

		if (transactionDao.count() == 0) {
			Block genesisBlock = prepareGenesisBlock(genesisAccount);
			org.nem.core.dbmodel.Block b = populateGenesisBlock(genesisBlock, dbGenesisAccount);
			populateGenesisTxes(dbGenesisAccount, b, genesisBlock);
		}
	}

	private Block prepareGenesisBlock(Account genesisAccount) {
		final BigInteger genesisAmount = new BigInteger("40000000000000");
		final BigInteger special       = new BigInteger("10000000000000");
		final BigInteger share = genesisAmount.subtract(special).divide(BigInteger.valueOf(Genesis.RECIPIENT_IDS.length - 1));
		final long amounts[] = {
			special.longValue(),
			share.longValue(), share.longValue(), share.longValue(), share.longValue(),
			share.longValue(), share.longValue(), share.longValue(), share.longValue()
		};

		final long txIds[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		Vector<Account> recipientsAccounts = new Vector<>(txIds.length);
		for (int i = 0; i < txIds.length; ++i) {
			final Address recipientAddr = Address.fromEncoded(Genesis.RECIPIENT_IDS[i]);
			recipientsAccounts.add(new Account(recipientAddr));
		}

		byte[] previousBlockHash = new byte[32];

		Block genesisBlock = new Block(genesisAccount, previousBlockHash, Genesis.INITIAL_TIME, Genesis.INITIAL_HEIGHT);
		for (int i = 0; i < txIds.length; ++i) {
			final TransferTransaction transferTransaction = new TransferTransaction(genesisAccount, recipientsAccounts.get(i), amounts[i], null);
			transferTransaction.setFee(0);
			transferTransaction.sign();

			genesisBlock.addTransaction(transferTransaction);
		}

		genesisBlock.sign();

		return genesisBlock;
	}

	private void populateGenesisTxes(org.nem.core.dbmodel.Account a, org.nem.core.dbmodel.Block b, Block genesisBlock) {
		if (transactionDao.count() == 0) {
			int transactionsCount = genesisBlock.getTransactions().size();
			Vector<org.nem.core.dbmodel.Account> recipientsDbAccounts = new Vector<>(transactionsCount);

			// recipients - add to or get from the db
			if (accountDao.count() == 1) {
				for (Transaction transaction : genesisBlock.getTransactions()) {
					final TransferTransaction transferTransaction = (TransferTransaction)transaction;
					final Account recipient = transferTransaction.getRecipient();
					final Address recipientAddr = recipient.getAddress();

					recipientsDbAccounts.add(new org.nem.core.dbmodel.Account(recipientAddr.getEncoded(), null));
				}
				accountDao.saveMulti(recipientsDbAccounts);

			} else {
				for (Transaction transaction : genesisBlock.getTransactions()) {
					final TransferTransaction transferTransaction = (TransferTransaction)transaction;
					final Address recipientAddr = transferTransaction.getRecipient().getAddress();

					recipientsDbAccounts.add(accountDao.getAccountByPrintableAddress(recipientAddr.getEncoded()));
				}
			}

			final long txIds[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
			Vector<Transfer> transactions = new Vector<>(transactionsCount);
			int i = 0;
			for (Transaction transaction : genesisBlock.getTransactions()) {
				final TransferTransaction transferTransaction = (TransferTransaction)transaction;

				byte[] hash = transferTransaction.getHash();
				Transfer t = new Transfer(
						ByteUtils.bytesToLong(hash),
						hash,
						transferTransaction.getVersion(),
						transferTransaction.getType(),
						0L, // can't use getFee here, as it does Min, transferTransaction.getFee(),
						Genesis.INITIAL_TIME, // timestamp
						0, // deadline
						a,
						// proof
						transferTransaction.getSignature().getBytes(),
						recipientsDbAccounts.get(i),
						i, // index
						transferTransaction.getAmount(),
						0L // referenced tx
				);
				t.setBlock(b);
				transactions.add(t);

				i++;
			}
			transactionDao.saveMulti(transactions);
		}
	}

	private org.nem.core.dbmodel.Block populateGenesisBlock(Block genesisBlock, org.nem.core.dbmodel.Account a) {
		org.nem.core.dbmodel.Block b = null;
		System.out.println(HexEncoder.getString(genesisBlock.getHash()));
		System.out.println(ByteUtils.bytesToLong(genesisBlock.getHash()));

		if (blockDao.count() == 0) {
			b = new org.nem.core.dbmodel.Block(
					Genesis.BLOCK_ID,
					1,
					// prev hash
					new byte[] {
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
					},
					genesisBlock.getHash(),
					0, // timestamp 
					a,
					// proof
					genesisBlock.getSignature().getBytes(),
					// block sig
					new byte[] {
						0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,
						0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,
						0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,
						0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0
					},
					0L, // height
					40000000L * 1000000L,
					(new BigInteger("0")).longValue()
					);
			blockDao.save(b);

		} else {
			b = blockDao.findByShortId(Genesis.BLOCK_ID);
		}

		logger.info("block id: " + b.getId().toString());
		return b;
	}

	private Account getGenesisAccount() {
		final KeyPair CREATOR_KEYPAIR = new KeyPair(Genesis.CREATOR_PRIVATE_KEY);

		return new Account(CREATOR_KEYPAIR);
	}

	private org.nem.core.dbmodel.Account populateGenesisAccount(Account genesisAccount) {
		final byte[] genesisPublicKey = genesisAccount.getKeyPair().getPublicKey();
		final Address genesisAddress = Address.fromPublicKey(genesisPublicKey);

		logger.info("genedis account private key: " + HexEncoder.getString(genesisAccount.getKeyPair().getPrivateKey().toByteArray()));
		logger.info("genesis account            public key: " + HexEncoder.getString(genesisPublicKey));
		logger.info("genesis account compressed public key: " + genesisAddress.getEncoded());

		org.nem.core.dbmodel.Account a = null;
		if (accountDao.count() == 0) {
			a = new org.nem.core.dbmodel.Account(genesisAddress.getEncoded(), genesisPublicKey);
			accountDao.save(a);

		} else {
			logger.warning("account counts: " + accountDao.count().toString());
			a = accountDao.getAccountByPrintableAddress(genesisAddress.getEncoded());
		}

		logger.info("account id: " + a.getId().toString());
		return a;
	}
}
