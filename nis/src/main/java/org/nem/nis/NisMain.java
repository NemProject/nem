package org.nem.nis;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Vector;

import javax.annotation.PostConstruct;

import java.util.logging.Logger;

import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.Hashes;
import org.nem.core.model.Transaction;
import org.nem.core.utils.StringEncoder;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.model.Account;
import org.nem.nis.model.Block;
import org.nem.nis.model.Transfer;
import org.nem.peer.PeerInitializer;
import org.nem.core.model.Address;
import org.springframework.beans.factory.annotation.Autowired;


public class NisMain
{
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
    
    public static int getEpochTime(long time)
	{
		return (int)((time - epochBeginning + 500L) / 1000L);
	}
    
    private void analyzeBlocks()
    {
    	Long curBlockId = GENESIS_BLOCK_ID;
        Block curBlock;
        System.out.println("starting analysis...");
        while ((curBlock = blockDao.findByShortId(curBlockId)) != null)
        {
        	blockAnalyzer.analyze(curBlock);
        	curBlockId = curBlock.getNextBlockId();
        	if (curBlockId == null) {
        		break;
        	}
        }    
    }
    
    
    
    @PostConstruct
    private void init() {
    	populateDb();
    	
    	blockAnalyzer = new BlockAnalyzer();
    	
    	analyzeBlocks();
    	initEpoch();
    	
    	Thread th = new PeerInitializer();
    	th.start();
    }
    
	private void populateDb() {
		Account a = populateGenesisAccount();
		
		Block b = populateGenesisBlock(a);
		
		populateGenesisTxes(a, b);
	}

	private void populateGenesisTxes(Account a, Block b) {
		if (transactionDao.count() == 0) {
			final long txIds[] = {
					1, 2, 3, 4, 5, 6, 7, 8, 9
			};
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
			
			Vector<Account> recipientsAccounts = new Vector<Account>(txIds.length);
			if (accountDao.count() == 1) {
				for (int i=0; i<txIds.length; ++i) {
                    final BigInteger recipientSecret = new BigInteger( recipientsSk[i] );
                    final KeyPair recipientKeypair = new KeyPair(recipientSecret);
                    final byte[] recipientPk = recipientKeypair.getPublicKey();
                    final Address recipientAddr = Address.fromPublicKey(recipientPk);
                    byte[] recipientAddrBase32 = recipientAddr.getEncoded().getBytes();

					recipientsAccounts.add(new Account(recipientAddrBase32, recipientPk));
				}
				accountDao.saveMulti(recipientsAccounts);
				
			} else {
				for (int i=0; i<txIds.length; ++i) {
                    final BigInteger recipientSecret = new BigInteger( recipientsSk[i] );
                    final KeyPair recipientKeypair = new KeyPair(recipientSecret);
                    final byte[] recipientPk = recipientKeypair.getPublicKey();
                    final Address recipientAddr = Address.fromPublicKey(recipientPk);
                    byte[] recipientAddrBase32 = recipientAddr.getEncoded().getBytes();

					recipientsAccounts.add(accountDao.getAccountByPrintableAddress(recipientAddrBase32));
				}
			}
			
			Vector<Transfer> transactions = new Vector<Transfer>(txIds.length);
			for (int i=0; i<txIds.length; ++i) {
				Transfer t = new Transfer(
						txIds[i],
						1, 0x1001,
						0L, // fee
						0, // timestamp
						0,
						a,
						// proof
						new byte[] {
								0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf,
								0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf,
								0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf,
								0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf
						},
						recipientsAccounts.get(i),
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

	private Block populateGenesisBlock(Account a) {
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

	private Account populateGenesisAccount() {
		final String CREATOR_PASS = "Remember, remember, the fifth of November, Gunpowder Treason and Plot";
        final BigInteger CREATOR_PRIVATE_KEY = new BigInteger( Hashes.sha3(StringEncoder.getBytes(CREATOR_PASS)) );
        final KeyPair CREATOR_KEYPAIR = new KeyPair(CREATOR_PRIVATE_KEY);
        final byte[] CREATOR_PUBLIC_KEY = CREATOR_KEYPAIR.getPublicKey();
        final Address CREATOR_ADDRESS = Address.fromPublicKey(CREATOR_PUBLIC_KEY);

        logger.info("genesis account public key: " + CREATOR_ADDRESS.getEncoded());

        // will get changed, but I want to wait for thies changes first
        byte base32Address[] = CREATOR_ADDRESS.getEncoded().getBytes();

		Account a = null;
		if (accountDao.count() == 0) {
            a = new Account(base32Address, CREATOR_PUBLIC_KEY);
			accountDao.save(a);
				
		} else {
			logger.warning("account counts: " + accountDao.count().toString());
			a = accountDao.getAccountByPrintableAddress(base32Address);
		}
		
		logger.info("account id: " + a.getId().toString());
		return a;
	}

//	private void unusedThread() {
//
//    	Thread th = new Thread(new Runnable() {
//    		private long checkMilestoneBlockIds() {
//				JSONObject obj;
//				// get IDs of milestone blocks, returned IDs are from
//				// "newest", so usually, the loop below will end pretty quickly
//				
//				long commonBlockId = GENESIS_BLOCK_ID;
//				
//				obj = NxtRequests.getMilestoneBlockIds();
//				obj = NxtRequests.makeRequest(obj);
//				JSONArray milestoneBlockIds = (JSONArray)obj.get("milestoneBlockIds");
//				for(Iterator<Object> i = milestoneBlockIds.iterator(); i.hasNext(); ) {
//					BigInteger mbId = new BigInteger((String)i.next());
//					
//					Block b = blockDao.findByShortId(mbId.longValue());
//					if (b != null) {
//						commonBlockId = mbId.longValue();
//						break;
//					}
//				}
//				return commonBlockId;
//			}
//
//			private long checkCommonBlockId(long commonBlockId) {
//				JSONObject obj;
//				// we've found common milestone block, start there and check how much more
//				// blocks we have in common with that peer
//				//
//				// next block IDs are returned "ordered"
//				//
//				logger.info("getting next block IDs, starting from: " + commonBlockId);
//				obj = NxtRequests.getNextBlockIds(commonBlockId);
//				obj = NxtRequests.makeRequest(obj);
//				JSONArray nextBlockIds = (JSONArray)obj.get("nextBlockIds");
//				for(Iterator<Object> i = nextBlockIds.iterator(); i.hasNext(); ) {
//					BigInteger tempBlockId = new BigInteger((String)i.next());
//					
//					Block b = blockDao.findByShortId(tempBlockId.longValue());
//					if (b == null) {
//						break;
//					}
//					
//					commonBlockId = tempBlockId.longValue();
//				}
//				
//				return commonBlockId;
//			}
//			
//			private void processRecievedBlocks(long commonBlockId) {
//				JSONObject obj;
//				obj = NxtRequests.getNextBlocks(commonBlockId);
//				obj = NxtRequests.makeRequest(obj);
//				
//				Block commonBlock = blockDao.findByShortId(commonBlockId);
//				VirtualBlockChain bc = new VirtualBlockChain(commonBlock);
//				VirtualAccounts ac = new VirtualAccounts();
//				
//				JSONArray nextBlocks = (JSONArray)obj.get("nextBlocks");
//				for(Iterator<Object> i = nextBlocks.iterator(); i.hasNext(); ) {
//					JSONObject jsonBlock = (JSONObject)i.next();
//					
//					System.out.println(jsonBlock);
//					if ((Integer)jsonBlock.get("version") == 1) {
//						if ((Integer)jsonBlock.get("numberOfTransactions") == 0) {
//							NrsBlock nrsBlock = NrsBlock.getBlock(jsonBlock);
//							
//							long generatorId = org.nxt.nrs.Utils.pk2Id( nrsBlock.getGeneratorPublicKey() );
//							Account generatorAcct = accountDao.getAccountByShortId(generatorId);
//							
//							if (generatorAcct == null) {
//								generatorAcct = new Account(generatorId, nrsBlock.getGeneratorPublicKey());
//								
//							} else {
//								if (generatorAcct.getPublicKey() == null) {
//									generatorAcct.setPublicKey(nrsBlock.getGeneratorPublicKey());
//								}
//							}
//							ac.add(generatorAcct);
//							//accountDao.save(generatorAcct);
//							
//							System.out.print("checking account: ");
//							System.out.println(jsonBlock.get("generatorPublicKey"));
//							System.out.println(generatorAcct.getId());
//							System.out.println(generatorAcct.getShortId());
//							System.out.println(generatorAcct.getPublicKey());
//							
//							if (blockDao.findByShortId(nrsBlock.getId()) != null) {
//								throw new RuntimeException("handle already present block");
//							}
//							
//							bc.add(nrsBlock,
//								new Block(
//									nrsBlock.getId(),
//									0L,
//									nrsBlock.getVersion(),
//									nrsBlock.getTimestamp(),
//									nrsBlock.getTotalAmount(),
//									nrsBlock.getTotalFee(),
//									generatorAcct.getId(),
//									nrsBlock.getGenerationSignature(),
//									nrsBlock.getBlockSignature(),
//									new byte[] {
//										0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
//										0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
//									}
//								),
//								generatorAcct
//							);
//							
//						} else {
//							throw new RuntimeException("block with transactions");
//						}
//						
//					} else {
//						throw new RuntimeException("block with unhandled version");
//					}
//					System.out.println(jsonBlock);
//					
//					break;
//				}
//			}
//    		public void run() {
//				
//				System.out.println("session opened");
//
//				JSONObject obj = NxtRequests.getInfo(); 
//				NxtRequests.makeRequest(obj);
//				
//				obj = NxtRequests.getPeers();
//				obj = NxtRequests.makeRequest(obj);
//				JSONArray peers = (JSONArray)obj.get("peers");
//				for(Iterator<Object> i = peers.iterator(); i.hasNext(); ) {
//					String peerAddress = (String)i.next();
//					System.out.println(peerAddress);
//				}
//				
//				obj = NxtRequests.getCumulativeDifficulty();
//				obj = NxtRequests.makeRequest(obj);
//				String cumulativeDiff = (String)obj.get("cumulativeDifficulty");
//				System.out.println(cumulativeDiff);
//				BigInteger peerCumulativeDifficulty = new BigInteger(cumulativeDiff);
//				if (peerCumulativeDifficulty.compareTo(cumulativeDifficulty) > 0){
//					
//					long commonBlockId = checkMilestoneBlockIds();
//					
//					commonBlockId = checkCommonBlockId(commonBlockId);
//					
//					processRecievedBlocks(commonBlockId);
//				}
//			}
//		});
//		
//		//th.start();
//	}
}
