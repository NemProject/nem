package org.nem.nis;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Vector;

import javax.annotation.PostConstruct;

import java.util.logging.Logger;

import org.nem.core.crypto.KeyPair;
import org.nem.core.dao.AccountDao;
import org.nem.core.dao.BlockDao;

import org.nem.core.dao.TransferDao;
import org.nem.core.model.*;
import org.nem.core.time.*;
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
	private static final Logger LOGGER = Logger.getLogger(NisMain.class.getName());

    public static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();
    public static final EntityFactory ENTITY_FACTORY = new EntityFactory(TIME_PROVIDER);

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private TransferDao transactionDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	@Autowired
	private BlockChain blockChain;

	public NisMain() {
	}

	private void analyzeBlocks() {
		Long curBlockId = 0L;
		System.out.println("starting analysis...");
		org.nem.core.dbmodel.Block curBlock = blockDao.findByHash(GenesisBlock.GENESIS_HASH);
		do {
			accountAnalyzer.analyze(curBlock);

			curBlockId = curBlock.getNextBlockId();
			if (curBlockId == null) {
				blockChain.analyzeLastBlock(curBlock);
				break;
			}
		} while ((curBlock = blockDao.findById(curBlockId)) != null);
	}

	@PostConstruct
	private void init() {
		LOGGER.warning("context ================== current: " + Long.toHexString(TIME_PROVIDER.getCurrentTime()));

		populateDb();

		analyzeBlocks();

		PeerNetworkHost peerNetworkHost = PeerNetworkHost.getDefaultHost();

		blockChain.bootup();
	}

	private void populateDb() {
		org.nem.core.dbmodel.Account dbGenesisAccount = populateGenesisAccount(GenesisBlock.GENESIS_ACCOUNT);

		if (transactionDao.count() == 0) {
			Block genesisBlock = prepareGenesisBlock();

			LOGGER.info("genesisBlockHash: " + HexEncoder.getString(HashUtils.calculateHash(genesisBlock)));

			org.nem.core.dbmodel.Block b = populateGenesisBlock(genesisBlock, dbGenesisAccount);
			populateGenesisTxes(dbGenesisAccount, b, genesisBlock);
		}
	}

	private Block prepareGenesisBlock() {
		return new GenesisBlock(0);
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

				byte[] hash = HashUtils.calculateHash(transferTransaction);
				Transfer t = new Transfer(
						ByteUtils.bytesToLong(hash),
						hash,
						transferTransaction.getVersion(),
						transferTransaction.getType(),
						0L, // can't use getFee here, as it does Min, transferTransaction.getFee(),
						genesisBlock.getTimeStamp(),
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
        byte[] genesisBlockHash = HashUtils.calculateHash(genesisBlock);

		System.out.println("aaa: " + HexEncoder.getString(genesisBlockHash));
		System.out.println("bbb: " + HexEncoder.getString(GenesisBlock.GENESIS_HASH));
		System.out.println(ByteUtils.bytesToLong(genesisBlockHash));

		if (blockDao.count() == 0) {
			b = new org.nem.core.dbmodel.Block(
					ByteUtils.bytesToLong(GenesisBlock.GENESIS_HASH),
					1,
					// prev hash
					new byte[] {
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
					},
                    genesisBlockHash,
					0, // timestamp 
					a,
					// proof
					genesisBlock.getSignature().getBytes(),
					0L, // height
					40000000L * 1000000L,
					0L
					);
			blockDao.save(b);

		} else {
			b = blockDao.findByHash(GenesisBlock.GENESIS_HASH);
		}

		LOGGER.info("block id: " + b.getId().toString());
		return b;
	}

	private org.nem.core.dbmodel.Account populateGenesisAccount(Account genesisAccount) {
		final byte[] genesisPublicKey = genesisAccount.getKeyPair().getPublicKey();
		final Address genesisAddress = Address.fromPublicKey(genesisPublicKey);

		LOGGER.info("genesis account private key: " + HexEncoder.getString(genesisAccount.getKeyPair().getPrivateKey().toByteArray()));
		LOGGER.info("genesis account            public key: " + HexEncoder.getString(genesisPublicKey));
		LOGGER.info("genesis account compressed public key: " + genesisAddress.getEncoded());

		org.nem.core.dbmodel.Account a = null;
		if (accountDao.count() == 0) {
			a = new org.nem.core.dbmodel.Account(genesisAddress.getEncoded(), genesisPublicKey);
			accountDao.save(a);

		} else {
			LOGGER.warning("account counts: " + accountDao.count().toString());
			a = accountDao.getAccountByPrintableAddress(genesisAddress.getEncoded());
		}

		LOGGER.info("account id: " + a.getId().toString());
		return a;
	}
}
