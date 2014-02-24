package org.nem.nis;

import java.io.IOException;
import java.math.BigInteger;
import java.security.CryptoPrimitive;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.nem.deploy.WebStarter;
import org.nem.nis.crypto.Hashes;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.model.Account;
import org.nem.nis.model.Block;
import org.nem.nis.model.Transfer;
import org.nem.nis.virtual.VirtualAccounts;
import org.nem.nis.virtual.VirtualBlockChain;
import org.nem.peer.PeerInitializer;
import org.nem.peer.PeerNetwork;
import org.nxt.nrs.Crypto;
import org.nxt.nrs.NrsBlock;
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
		calendar.set(Calendar.YEAR, 2013);
		calendar.set(Calendar.MONTH, 10);
		calendar.set(Calendar.DAY_OF_MONTH, 24);
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
		Account a = populateGenesisAccount();

		Block b = populateGenesisBlock(a);

		populateGenesisTxes(a, b);
	}

	private void populateGenesisTxes(Account a, Block b) {
		if (transactionDao.count() == 0) {
			final long txIds[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
			// super strong priv keys
			final byte[] recipientsSk[] = { Hashes.sha256(Converter.stringToBytes("super-duper-special")),
					Hashes.sha256(Converter.stringToBytes("Jaguar0625")), Hashes.sha256(Converter.stringToBytes("BloodyRookie")),
					Hashes.sha256(Converter.stringToBytes("Thies1965")), Hashes.sha256(Converter.stringToBytes("borzalom")),
					Hashes.sha256(Converter.stringToBytes("gimre")), Hashes.sha256(Converter.stringToBytes("Makoto")),
					Hashes.sha256(Converter.stringToBytes("UtopianFuture")), Hashes.sha256(Converter.stringToBytes("minusbalancer")) };
			final long amounts[] = { (new BigInteger("10000000000000")).longValue(), (new BigInteger("3750000000000")).longValue(),
					(new BigInteger("3750000000000")).longValue(), (new BigInteger("3750000000000")).longValue(),
					(new BigInteger("3750000000000")).longValue(), (new BigInteger("3750000000000")).longValue(),
					(new BigInteger("3750000000000")).longValue(), (new BigInteger("3750000000000")).longValue(),
					(new BigInteger("3750000000000")).longValue() };

			Vector<Account> recipientsAccounts = new Vector<Account>(txIds.length);
			if (accountDao.count() == 1) {
				for (int i = 0; i < txIds.length; ++i) {
					byte[] recipientPk = Crypto.getPublicKey(recipientsSk[i]);
					Address recipientAddr = new Address(Address.MAIN_NET, Address.VERSION, recipientPk);

					recipientsAccounts.add(new Account(recipientAddr.getBase32Address(), recipientPk));
				}
				accountDao.saveMulti(recipientsAccounts);

			} else {
				for (int i = 0; i < txIds.length; ++i) {
					byte[] recipientPk = Crypto.getPublicKey(recipientsSk[i]);
					Address recipientAddr = new Address(Address.MAIN_NET, Address.VERSION, recipientPk);

					recipientsAccounts.add(accountDao.getAccountByPrintableAddress(recipientAddr.getBase32Address()));
				}
			}

			Vector<Transfer> transactions = new Vector<Transfer>(txIds.length);
			for (int i = 0; i < txIds.length; ++i) {
				Transfer t = new Transfer(txIds[i], 1, 0x1001, 0L, // fee
						0, // timestamp
						0, a,
						// proof
						new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xa, 0xb,
								0xc, 0xd, 0xe, 0xf, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0, 1, 2, 3, 4, 5, 6, 7, 8,
								9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf }, recipientsAccounts.get(i), i, // index
						amounts[i], 0L // referenced tx
				);
				t.setBlock(b);
				transactions.add(t);
			}
			transactionDao.saveMulti(transactions);
		}
	}

	private Block populateGenesisBlock(Account a) {
		Block b = null;
		if (blockDao.count() == 0) {

			b = new Block(GENESIS_BLOCK_ID, 1,
			// prev hash
					new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
					// current block hash
					new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, // timestamp
					a,
					// proof
					new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
							0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
					// block sig
					new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
							0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0L, // height
					40000000L * 1000000L, (new BigInteger("0")).longValue());
			blockDao.save(b);

		} else {
			b = blockDao.findByShortId(GENESIS_BLOCK_ID);
		}

		logger.info("block id: " + b.getId().toString());
		return b;
	}

	private Account populateGenesisAccount() {
		final String CREATOR_PASS = "Remember, remember, the fifth of November, Gunpowder Treason and Plot";
		final byte[] CREATOR_PRIVATE_KEY = Hashes.sha256(Converter.stringToBytes(CREATOR_PASS));
		final byte[] CREATOR_PUBLIC_KEY = Crypto.getPublicKey(CREATOR_PRIVATE_KEY);
		final Address CREATOR_ADDRESS = new Address(Address.MAIN_NET, Address.VERSION, CREATOR_PUBLIC_KEY);

		Account a = null;
		if (accountDao.count() == 0) {
			a = new Account(CREATOR_ADDRESS.getBase32Address(), CREATOR_PUBLIC_KEY);
			accountDao.save(a);

		} else {
			logger.warning("account counts: " + accountDao.count().toString());
			a = accountDao.getAccountByPrintableAddress(CREATOR_ADDRESS.getBase32Address());
		}

		logger.info("account id: " + a.getId().toString());
		return a;
	}

	// private void unusedThread() {
	//
	// Thread th = new Thread(new Runnable() {
	// private long checkMilestoneBlockIds() {
	// JSONObject obj;
	// // get IDs of milestone blocks, returned IDs are from
	// // "newest", so usually, the loop below will end pretty quickly
	//
	// long commonBlockId = GENESIS_BLOCK_ID;
	//
	// obj = NxtRequests.getMilestoneBlockIds();
	// obj = NxtRequests.makeRequest(obj);
	// JSONArray milestoneBlockIds = (JSONArray)obj.get("milestoneBlockIds");
	// for(Iterator<Object> i = milestoneBlockIds.iterator(); i.hasNext(); ) {
	// BigInteger mbId = new BigInteger((String)i.next());
	//
	// Block b = blockDao.findByShortId(mbId.longValue());
	// if (b != null) {
	// commonBlockId = mbId.longValue();
	// break;
	// }
	// }
	// return commonBlockId;
	// }
	//
	// private long checkCommonBlockId(long commonBlockId) {
	// JSONObject obj;
	// // we've found common milestone block, start there and check how much
	// more
	// // blocks we have in common with that peer
	// //
	// // next block IDs are returned "ordered"
	// //
	// logger.info("getting next block IDs, starting from: " + commonBlockId);
	// obj = NxtRequests.getNextBlockIds(commonBlockId);
	// obj = NxtRequests.makeRequest(obj);
	// JSONArray nextBlockIds = (JSONArray)obj.get("nextBlockIds");
	// for(Iterator<Object> i = nextBlockIds.iterator(); i.hasNext(); ) {
	// BigInteger tempBlockId = new BigInteger((String)i.next());
	//
	// Block b = blockDao.findByShortId(tempBlockId.longValue());
	// if (b == null) {
	// break;
	// }
	//
	// commonBlockId = tempBlockId.longValue();
	// }
	//
	// return commonBlockId;
	// }
	//
	// private void processRecievedBlocks(long commonBlockId) {
	// JSONObject obj;
	// obj = NxtRequests.getNextBlocks(commonBlockId);
	// obj = NxtRequests.makeRequest(obj);
	//
	// Block commonBlock = blockDao.findByShortId(commonBlockId);
	// VirtualBlockChain bc = new VirtualBlockChain(commonBlock);
	// VirtualAccounts ac = new VirtualAccounts();
	//
	// JSONArray nextBlocks = (JSONArray)obj.get("nextBlocks");
	// for(Iterator<Object> i = nextBlocks.iterator(); i.hasNext(); ) {
	// JSONObject jsonBlock = (JSONObject)i.next();
	//
	// System.out.println(jsonBlock);
	// if ((Integer)jsonBlock.get("version") == 1) {
	// if ((Integer)jsonBlock.get("numberOfTransactions") == 0) {
	// NrsBlock nrsBlock = NrsBlock.getBlock(jsonBlock);
	//
	// long generatorId = org.nxt.nrs.Utils.pk2Id(
	// nrsBlock.getGeneratorPublicKey() );
	// Account generatorAcct = accountDao.getAccountByShortId(generatorId);
	//
	// if (generatorAcct == null) {
	// generatorAcct = new Account(generatorId,
	// nrsBlock.getGeneratorPublicKey());
	//
	// } else {
	// if (generatorAcct.getPublicKey() == null) {
	// generatorAcct.setPublicKey(nrsBlock.getGeneratorPublicKey());
	// }
	// }
	// ac.add(generatorAcct);
	// //accountDao.save(generatorAcct);
	//
	// System.out.print("checking account: ");
	// System.out.println(jsonBlock.get("generatorPublicKey"));
	// System.out.println(generatorAcct.getId());
	// System.out.println(generatorAcct.getShortId());
	// System.out.println(generatorAcct.getPublicKey());
	//
	// if (blockDao.findByShortId(nrsBlock.getId()) != null) {
	// throw new RuntimeException("handle already present block");
	// }
	//
	// bc.add(nrsBlock,
	// new Block(
	// nrsBlock.getId(),
	// 0L,
	// nrsBlock.getVersion(),
	// nrsBlock.getTimestamp(),
	// nrsBlock.getTotalAmount(),
	// nrsBlock.getTotalFee(),
	// generatorAcct.getId(),
	// nrsBlock.getGenerationSignature(),
	// nrsBlock.getBlockSignature(),
	// new byte[] {
	// 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	// 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
	// }
	// ),
	// generatorAcct
	// );
	//
	// } else {
	// throw new RuntimeException("block with transactions");
	// }
	//
	// } else {
	// throw new RuntimeException("block with unhandled version");
	// }
	// System.out.println(jsonBlock);
	//
	// break;
	// }
	// }
	// public void run() {
	//
	// System.out.println("session opened");
	//
	// JSONObject obj = NxtRequests.getInfo();
	// NxtRequests.makeRequest(obj);
	//
	// obj = NxtRequests.getPeers();
	// obj = NxtRequests.makeRequest(obj);
	// JSONArray peers = (JSONArray)obj.get("peers");
	// for(Iterator<Object> i = peers.iterator(); i.hasNext(); ) {
	// String peerAddress = (String)i.next();
	// System.out.println(peerAddress);
	// }
	//
	// obj = NxtRequests.getCumulativeDifficulty();
	// obj = NxtRequests.makeRequest(obj);
	// String cumulativeDiff = (String)obj.get("cumulativeDifficulty");
	// System.out.println(cumulativeDiff);
	// BigInteger peerCumulativeDifficulty = new BigInteger(cumulativeDiff);
	// if (peerCumulativeDifficulty.compareTo(cumulativeDifficulty) > 0){
	//
	// long commonBlockId = checkMilestoneBlockIds();
	//
	// commonBlockId = checkCommonBlockId(commonBlockId);
	//
	// processRecievedBlocks(commonBlockId);
	// }
	// }
	// });
	//
	// //th.start();
	// }
}
