package org.nem.nis;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Vector;

import javax.annotation.PostConstruct;

import java.util.logging.Logger;

import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.Hashes;
import org.nem.core.dao.AccountDao;
import org.nem.core.dao.BlockDao;

import org.nem.core.dao.TransferDao;
import org.nem.core.model.Account;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.HexEncoder;
import org.nem.core.utils.StringEncoder;
import org.nem.core.dbmodel.Block;
import org.nem.core.dbmodel.Transfer;
import org.nem.core.model.Address;
import org.nem.peer.PeerNetwork;
import org.springframework.beans.factory.annotation.Autowired;

public class NisMain {
	private static final Logger logger = Logger.getLogger(NisMain.class.getName());

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private TransferDao transactionDao;

	public NisMain() {
	}

	final long GENESIS_BLOCK_ID = 0x1234567890abcdefL;
	BlockAnalyzer blockAnalyzer;

	static long epochBeginning;

	static private void initEpoch() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.ERA, 0);
		calendar.set(Calendar.YEAR, 2014);
		calendar.set(Calendar.MONTH, 07);
		calendar.set(Calendar.DAY_OF_MONTH, 01);
		calendar.set(Calendar.HOUR, 12);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		epochBeginning = calendar.getTimeInMillis();
	}

	public static int getEpochTime(long time) {
		return (int) ((time - epochBeginning + 500L) / 1000L);
	}

	private void analyzeBlocks() {
		Long curBlockId = GENESIS_BLOCK_ID;
		Block curBlock;
		System.out.println("starting analysis...");
		while ((curBlock = blockDao.findByShortId(curBlockId)) != null) {
			blockAnalyzer.analyze(curBlock);
			curBlockId = curBlock.getNextBlockId();
			if (curBlockId == null) {
				break;
			}
		}
	}

	@PostConstruct
	private void init() {

		/** 
		 * Thies1965, something is still wrong with my set-up
		 * I get an SQL exception
		 * Just for my testing purposes, I commented the next lines out
		 */
		
		populateDb();

		blockAnalyzer = new BlockAnalyzer();

		analyzeBlocks();

		initEpoch();

		PeerNetwork peerNetwork = PeerNetwork.getDefaultNetwork();
		if (peerNetwork == null) {
			logger.severe("Cannot bring-up the PeerNetwork. Server is going down, no chance to work.");
			// No chance to be successful
			// Just for the moment we go down
			// very ugly
			System.exit(1);
		}
	}

	private void populateDb() {
		Account genesisAccount = getGenesisAccount();
		org.nem.core.dbmodel.Account dbGenesisAccout = populateGenesisAccount(genesisAccount);

		Block b = populateGenesisBlock(dbGenesisAccout);

		populateGenesisTxes(genesisAccount, dbGenesisAccout, b);
	}

	private void populateGenesisTxes(Account genesisAccount, org.nem.core.dbmodel.Account a, Block b) {
		if (transactionDao.count() == 0) {
			final long txIds[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
			// super strong priv keys
			final byte[] recipientsSk[] = {
					Hashes.sha3(StringEncoder.getBytes("super-duper-special")),
					Hashes.sha3(StringEncoder.getBytes("Jaguar0625")),
					Hashes.sha3(StringEncoder.getBytes("BloodyRookie")),
					Hashes.sha3(StringEncoder.getBytes("Thies1965")),
					Hashes.sha3(StringEncoder.getBytes("borzalom")),
					Hashes.sha3(StringEncoder.getBytes("gimre")),
					Hashes.sha3(StringEncoder.getBytes("Makoto")),
					Hashes.sha3(StringEncoder.getBytes("UtopianFuture")),
					Hashes.sha3(StringEncoder.getBytes("minusbalancer"))
			};
			final BigInteger genesisAmount = new BigInteger("40000000000000");
			final BigInteger special       = new BigInteger("10000000000000");
			final BigInteger share = genesisAmount.subtract(special).divide(BigInteger.valueOf(recipientsSk.length - 1));
			final long amounts[] = {
					special.longValue(),
					share.longValue(), share.longValue(), share.longValue(), share.longValue(),
					share.longValue(), share.longValue(), share.longValue(), share.longValue()
			};

			Vector<Account> recipientsAccounts = new Vector<>(txIds.length);
			Vector<org.nem.core.dbmodel.Account> recipientsDbAccounts = new Vector<>(txIds.length);
			if (accountDao.count() == 1) {
				for (int i = 0; i < txIds.length; ++i) {
					final BigInteger recipientSecret = new BigInteger( recipientsSk[i] );
					final KeyPair recipientKeypair = new KeyPair(recipientSecret);
					final byte[] recipientPk = recipientKeypair.getPublicKey();
					final Address recipientAddr = Address.fromPublicKey(recipientPk);

					recipientsAccounts.add(new Account(recipientKeypair));
					recipientsDbAccounts.add(new org.nem.core.dbmodel.Account(recipientAddr.getEncoded(), recipientPk));
				}
				accountDao.saveMulti(recipientsDbAccounts);

			} else {
				for (int i = 0; i < txIds.length; ++i) {
					final BigInteger recipientSecret = new BigInteger( recipientsSk[i] );
					final KeyPair recipientKeypair = new KeyPair(recipientSecret);
					final byte[] recipientPk = recipientKeypair.getPublicKey();
					final Address recipientAddr = Address.fromPublicKey(recipientPk);

					recipientsDbAccounts.add(accountDao.getAccountByPrintableAddress(recipientAddr.getEncoded()));
				}
			}

			Vector<Transfer> transactions = new Vector<>(txIds.length);
			for (int i = 0; i < txIds.length; ++i) {
				TransferTransaction transferTx = new TransferTransaction(genesisAccount, recipientsAccounts.get(i), amounts[i], null);
				transferTx.setFee(0);

				transferTx.sign();

				logger.warning(transferTx.getSignature().getR().toString(16) + " " + transferTx.getSignature().getS().toString(16));

				Transfer t = new Transfer(
						txIds[i],
						transferTx.getVersion(),
						transferTx.getType(),
						transferTx.getFee(),
						0, // timestamp
						0, // deadline
						a,
						// proof
						new byte[] {
								0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf,
								0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf,
								0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf,
								0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf
						},
						recipientsDbAccounts.get(i),
						i, // index
						amounts[i],
						0L // referenced tx
				);
				t.setBlock(b);
				transactions.add(t);
			}
			transactionDao.saveMulti(transactions);
		}
	}

	private Block populateGenesisBlock(org.nem.core.dbmodel.Account a) {
		Block b = null;
		if (blockDao.count() == 0) {
			
			b = new Block(
					GENESIS_BLOCK_ID,
					1,
					// prev hash
					new byte[] {
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
					},
					// current block hash
					new byte[] {
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
						0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
					},
					0, // timestamp 
					a,
					// proof
					new byte[] {
						0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,
						0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,
						0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,
						0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0
					},
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
			b = blockDao.findByShortId(GENESIS_BLOCK_ID);
		}

		logger.info("block id: " + b.getId().toString());
		return b;
	}

	private Account getGenesisAccount() {
		final String CREATOR_PASS = "Remember, remember, the fifth of November, Gunpowder Treason and Plot";
		final BigInteger CREATOR_PRIVATE_KEY = new BigInteger( Hashes.sha3(StringEncoder.getBytes(CREATOR_PASS)) );
		final KeyPair CREATOR_KEYPAIR = new KeyPair(CREATOR_PRIVATE_KEY);

		return new Account(CREATOR_KEYPAIR);
	}
	private org.nem.core.dbmodel.Account populateGenesisAccount(Account genesisAccount) {
		final byte[] genesisPublicKey = genesisAccount.getKeyPair().getPublicKey();
		final Address genesisAddress = Address.fromPublicKey(genesisPublicKey);

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
