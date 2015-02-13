package org.nem.nis.dao;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.*;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class that loads blocks from the database.
 * <br>
 * This class is used as an implementation detail of BlockDao and is tested mainly through those tests.
 */
public class BlockLoader {
	private static final Logger LOGGER = Logger.getLogger(BlockLoader.class.getName());
	private final static String[] multisigSignaturesColumns = {
			"multisigtransactionid", "id", "transferhash", "version", "fee", "timestamp", "deadline", "senderid", "senderproof"	};
	private final static String[] multisigModificationsColumns = {
			"multisigsignermodificationid", "id", "cosignatoryid", "modificationtype" };

	private final SessionFactory sessionFactory;
	private final HashMap<Long, DbBlock> dbBlockMap = new HashMap<>();
	private final HashMap<Long, DbTransferTransaction> multisigDbTransfers = new HashMap<>();
	private final HashMap<Long, DbImportanceTransferTransaction> multisigDbImportanceTransfers = new HashMap<>();
	private final HashMap<Long, DbMultisigAggregateModificationTransaction> multisigDbModificationTransactions = new HashMap<>();

	private long start = 0L;

	/**
	 * Creates a new block analyzer.
	 *
	 * @param sessionFactory The session factory.
	 */
	public BlockLoader(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	/**
	 * Loads blocks from the database.
	 *
	 * @param fromHeight The height from which on to pull blocks.
	 * @param toHeight The height up to which blocks should be pulled.
	 * @return The list of db blocks.
	 */
	public List<DbBlock> loadBlocks(final BlockHeight fromHeight, final BlockHeight toHeight) {
		final List<DbBlock> dbBlocks = this.getDbBlocks(fromHeight, toHeight);
		if (dbBlocks.isEmpty()) {
			return new ArrayList<>();
		}

		dbBlocks.stream().forEach(b -> this.dbBlockMap.put(b.getId(), b));
		final long minBlockId = dbBlocks.get(0).getId() - 1;
		final long maxBlockId = dbBlocks.get(dbBlocks.size() - 1).getId() + 1;
		final List<DbTransferTransaction> allDbTransfers = this.getDbTransfers(minBlockId, maxBlockId);
		this.extractMultisigTransfers(allDbTransfers, this.multisigDbTransfers);
		final List<DbImportanceTransferTransaction> allDbImportanceTransfers = this.getDbImportanceTransfers(minBlockId, maxBlockId);
		this.extractMultisigTransfers(allDbImportanceTransfers, this.multisigDbImportanceTransfers);
		final List<DbMultisigAggregateModificationTransaction> allDbModificationTransactions = this.getDbModificationTransactions(minBlockId, maxBlockId);
		this.extractMultisigTransfers(allDbModificationTransactions, this.multisigDbModificationTransactions);
		final List<DbMultisigTransaction> dbMultisigTransactions = this.getDbMultisigTransactions(minBlockId, maxBlockId);
		final HashSet<Long> accountIds = this.collectAccountIds(
				dbBlocks,
				allDbTransfers,
				allDbImportanceTransfers,
				allDbModificationTransactions,
				dbMultisigTransactions);
		final HashMap<Long, DbAccount> accountMap = this.getAccountMap(accountIds);
		this.updateAccounts(
				dbBlocks,
				allDbTransfers,
				allDbImportanceTransfers,
				allDbModificationTransactions,
				dbMultisigTransactions,
				accountMap);
		this.addTransactions(allDbTransfers, DbBlock::addTransferTransaction);
		this.addTransactions(allDbImportanceTransfers, DbBlock::addImportanceTransferTransaction);
		this.addTransactions(allDbModificationTransactions, DbBlock::addMultisigAggregateModificationTransaction);
		this.addTransactions(dbMultisigTransactions, DbBlock::addMultisigTransaction);


		return dbBlocks;
	}

	private List<DbBlock> getDbBlocks(final BlockHeight fromHeight, final BlockHeight toHeight) {
		this.actionStarts();
		final Query query = this.getCurrentSession()
				.createSQLQuery("SELECT b.* FROM BLOCKS b WHERE height > :fromHeight AND height <= :toHeight ORDER BY height ASC LIMIT :limit")
				.setParameter("fromHeight", fromHeight.getRaw())
				.setParameter("toHeight", toHeight.getRaw())
				.setParameter("limit", toHeight.getRaw() - fromHeight.getRaw());
		final List<Object[]> objects = listAndCast(query);
		this.actionEnds("getDbBlocks ");
		return objects.stream().map(this::mapToDbBlock).collect(Collectors.toList());
	}

	private DbBlock mapToDbBlock(final Object[] array) {
		final DbBlock dbBlock = new DbBlock();
		dbBlock.setId(castBigIntegerToLong((BigInteger)array[0]));
		dbBlock.setShortId(castBigIntegerToLong((BigInteger)array[1]));
		dbBlock.setVersion((Integer)array[2]);
		dbBlock.setPrevBlockHash(new Hash((byte[])array[3]));
		dbBlock.setBlockHash(new Hash((byte[])array[4]));
		dbBlock.setGenerationHash(new Hash((byte[])array[5]));
		dbBlock.setTimeStamp((Integer)array[6]);
		dbBlock.setHarvester(createDbAccount(castBigIntegerToLong((BigInteger)array[7])));
		dbBlock.setHarvesterProof((byte[])array[8]);
		dbBlock.setLessor(createDbAccount(castBigIntegerToLong((BigInteger)array[9])));
		dbBlock.setHeight(castBigIntegerToLong((BigInteger)array[10]));
		dbBlock.setTotalFee(castBigIntegerToLong((BigInteger)array[11]));
		dbBlock.setDifficulty(castBigIntegerToLong((BigInteger)array[12]));
		dbBlock.setBlockTransferTransactions(new ArrayList<>());
		dbBlock.setBlockImportanceTransferTransactions(new ArrayList<>());
		dbBlock.setBlockMultisigAggregateModificationTransactions(new ArrayList<>());
		dbBlock.setBlockMultisigTransactions(new ArrayList<>());

		return dbBlock;
	}

	private List<DbTransferTransaction> getDbTransfers(
			final long minBlockId,
			final long maxBlockId) {
		this.actionStarts();
		final String queryString = "SELECT t.* FROM transfers t " +
				"WHERE blockid > :minBlockId AND blockid < :maxBlockId " +
				"ORDER BY blockid ASC";
		final Query query = this.getCurrentSession()
				.createSQLQuery(queryString)
				.setParameter("minBlockId", minBlockId)
				.setParameter("maxBlockId", maxBlockId);
		final List<Object[]> objects = listAndCast(query);
		this.actionEnds("getDbTransfers ");
		return objects.stream().map(this::mapToDbTransfers).collect(Collectors.toList());
	}

	private DbTransferTransaction mapToDbTransfers(final Object[] array) {
		final DbTransferTransaction dbTransfer = new DbTransferTransaction();
		dbTransfer.setBlock(createDbBlock(castBigIntegerToLong((BigInteger)array[0])));
		dbTransfer.setId(castBigIntegerToLong((BigInteger)array[1]));
		dbTransfer.setTransferHash(new Hash((byte[])array[2]));
		dbTransfer.setVersion((Integer)array[3]);
		dbTransfer.setFee(castBigIntegerToLong((BigInteger)array[4]));
		dbTransfer.setTimeStamp((Integer)array[5]);
		dbTransfer.setDeadline((Integer)array[6]);
		dbTransfer.setSender(createDbAccount(castBigIntegerToLong((BigInteger)array[7])));
		dbTransfer.setSenderProof((byte[])array[8]);
		dbTransfer.setRecipient(createDbAccount(castBigIntegerToLong((BigInteger)array[9])));
		dbTransfer.setBlkIndex((Integer)array[10]);
		dbTransfer.setOrderId((Integer)array[11]);
		dbTransfer.setAmount(castBigIntegerToLong((BigInteger)array[12]));
		dbTransfer.setReferencedTransaction(castBigIntegerToLong((BigInteger)array[13]));
		dbTransfer.setMessageType((Integer)array[14]);
		dbTransfer.setMessagePayload((byte[])array[15]);

		return dbTransfer;
	}

	private List<DbImportanceTransferTransaction> getDbImportanceTransfers(
			final long minBlockId,
			final long maxBlockId) {
		this.actionStarts();
		final String queryString = "SELECT t.* FROM importancetransfers t " +
				"WHERE blockid > :minBlockId AND blockid < :maxBlockId " +
				"ORDER BY blockid ASC";
		final Query query = this.getCurrentSession()
				.createSQLQuery(queryString)
				.setParameter("minBlockId", minBlockId)
				.setParameter("maxBlockId", maxBlockId);
		final List<Object[]> objects = listAndCast(query);
		this.actionEnds("getDbImportanceTransfers ");
		return objects.stream().map(this::mapToDbImportanceTransfers).collect(Collectors.toList());
	}

	private DbImportanceTransferTransaction mapToDbImportanceTransfers(final Object[] array) {
		final DbImportanceTransferTransaction dbImportanceTransfer = new DbImportanceTransferTransaction();
		dbImportanceTransfer.setBlock(createDbBlock(castBigIntegerToLong((BigInteger)array[0])));
		dbImportanceTransfer.setId(castBigIntegerToLong((BigInteger)array[1]));
		dbImportanceTransfer.setTransferHash(new Hash((byte[])array[2]));
		dbImportanceTransfer.setVersion((Integer)array[3]);
		dbImportanceTransfer.setFee(castBigIntegerToLong((BigInteger)array[4]));
		dbImportanceTransfer.setTimeStamp((Integer)array[5]);
		dbImportanceTransfer.setDeadline((Integer)array[6]);
		dbImportanceTransfer.setSender(createDbAccount(castBigIntegerToLong((BigInteger)array[7])));
		dbImportanceTransfer.setSenderProof((byte[])array[8]);
		dbImportanceTransfer.setRemote(createDbAccount(castBigIntegerToLong((BigInteger)array[9])));
		dbImportanceTransfer.setMode((Integer)array[10]);
		dbImportanceTransfer.setBlkIndex((Integer)array[11]);
		dbImportanceTransfer.setOrderId((Integer)array[12]);
		dbImportanceTransfer.setReferencedTransaction(castBigIntegerToLong((BigInteger)array[13]));

		return dbImportanceTransfer;
	}

	private List<DbMultisigAggregateModificationTransaction> getDbModificationTransactions(
			final long minBlockId,
			final long maxBlockId) {
		this.actionStarts();
		final String columnList = this.createColumnList("mm", 1, multisigModificationsColumns);
		final String queryString =
				"SELECT msm.*, " + columnList + " FROM multisigsignermodifications msm " +
				"LEFT OUTER JOIN multisigmodifications mm ON mm.multisigsignermodificationid = msm.id " +
				"WHERE msm.blockid > :minBlockId AND msm.blockid < :maxBlockId " +
				"ORDER BY msm.blockid ASC";
		final Query query = this.getCurrentSession()
				.createSQLQuery(queryString)
				.setParameter("minBlockId", minBlockId)
				.setParameter("maxBlockId", maxBlockId);
		final List<Object[]> objects = listAndCast(query);
		this.actionEnds("getDbModificationTransactions ");
		return this.mapToDbModificationTransactions(objects);
	}

	private List<DbMultisigAggregateModificationTransaction> mapToDbModificationTransactions(final List<Object[]> arrays) {
		if (arrays.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigAggregateModificationTransaction> transactions = new ArrayList<>();
		DbMultisigAggregateModificationTransaction dbModificationTransaction = null;
		long curTxId = 0L;
		for (Object[] array : arrays) {
			final long txid = castBigIntegerToLong((BigInteger)array[12]);
			if (curTxId != txid) {
				curTxId = txid;
				dbModificationTransaction = this.mapToDbModificationTransaction(array);
				transactions.add(dbModificationTransaction);
			}

			dbModificationTransaction.getMultisigModifications().add(this.mapToDbModification(dbModificationTransaction, array));
		}

		return transactions;
	}

	private DbMultisigAggregateModificationTransaction mapToDbModificationTransaction(final Object[] array) {
		final DbMultisigAggregateModificationTransaction dbModificationTransaction = new DbMultisigAggregateModificationTransaction();
		dbModificationTransaction.setBlock(createDbBlock(castBigIntegerToLong((BigInteger)array[0])));
		dbModificationTransaction.setId(castBigIntegerToLong((BigInteger)array[1]));
		dbModificationTransaction.setTransferHash(new Hash((byte[])array[2]));
		dbModificationTransaction.setVersion((Integer)array[3]);
		dbModificationTransaction.setFee(castBigIntegerToLong((BigInteger)array[4]));
		dbModificationTransaction.setTimeStamp((Integer)array[5]);
		dbModificationTransaction.setDeadline((Integer)array[6]);
		dbModificationTransaction.setSender(createDbAccount(castBigIntegerToLong((BigInteger)array[7])));
		dbModificationTransaction.setSenderProof((byte[])array[8]);
		dbModificationTransaction.setBlkIndex((Integer)array[9]);
		dbModificationTransaction.setOrderId((Integer)array[10]);
		dbModificationTransaction.setReferencedTransaction(castBigIntegerToLong((BigInteger)array[11]));
		dbModificationTransaction.setMultisigModifications(new HashSet<>());

		return dbModificationTransaction;
	}

	private DbMultisigModification mapToDbModification(
			final DbMultisigAggregateModificationTransaction dbModificationTransaction,
			final Object[] array) {
		final DbMultisigModification dbModification = new DbMultisigModification();
		dbModification.setMultisigAggregateModificationTransaction(dbModificationTransaction);
		dbModification.setId(castBigIntegerToLong((BigInteger)array[13]));
		dbModification.setCosignatory(createDbAccount(castBigIntegerToLong((BigInteger)array[14])));
		dbModification.setModificationType((Integer)array[15]);

		return dbModification;
	}

	private List<DbMultisigTransaction> getDbMultisigTransactions(final long minBlockId, final long maxBlockId) {
		this.actionStarts();
		final String columnList = this.createColumnList("ms", 1, multisigSignaturesColumns);
		final String queryString = "SELECT mt.*, " + columnList + " FROM MULTISIGTRANSACTIONS mt " +
				"LEFT OUTER JOIN multisigsignatures ms on ms.multisigtransactionid = mt.id " +
				"WHERE mt.blockid > :minBlockId and mt.blockid < :maxBlockId " +
				"ORDER BY mt.blockid ASC";
		final Query query = this.getCurrentSession()
				.createSQLQuery(queryString)
				.setParameter("minBlockId", minBlockId)
				.setParameter("maxBlockId", maxBlockId);
		final List<Object[]> objects = listAndCast(query);
		this.actionEnds("getDbMultisigTransactions ");
		return this.mapToDbMultisigTransactions(objects);
	}

	private List<DbMultisigTransaction> mapToDbMultisigTransactions(final List<Object[]> arrays) {
		if (arrays.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigTransaction> transactions = new ArrayList<>();
		DbMultisigTransaction dbMultisigTransaction = null;
		long curTxId = 0L;
		for (Object[] array : arrays) {
			final Long txid = castBigIntegerToLong((BigInteger)array[15]);
			if (null == txid) {
				// no cosignatories
				dbMultisigTransaction = this.mapToDbMultsigTransaction(array);
				transactions.add(dbMultisigTransaction);
				continue;
			}

			if (curTxId != txid) {
				curTxId = txid;
				dbMultisigTransaction = this.mapToDbMultsigTransaction(array);
				transactions.add(dbMultisigTransaction);
			}

			dbMultisigTransaction.getMultisigSignatureTransactions().add(this.mapToDbMulsigSignature(dbMultisigTransaction, array));
		}

		return transactions;
	}

	private DbMultisigTransaction mapToDbMultsigTransaction(final Object[] array) {
		final DbMultisigTransaction dbMultsigTransaction = new DbMultisigTransaction();
		dbMultsigTransaction.setBlock(createDbBlock(castBigIntegerToLong((BigInteger)array[0])));
		dbMultsigTransaction.setId(castBigIntegerToLong((BigInteger)array[1]));
		dbMultsigTransaction.setTransferHash(new Hash((byte[])array[2]));
		dbMultsigTransaction.setVersion((Integer)array[3]);
		dbMultsigTransaction.setFee(castBigIntegerToLong((BigInteger)array[4]));
		dbMultsigTransaction.setTimeStamp((Integer)array[5]);
		dbMultsigTransaction.setDeadline((Integer)array[6]);
		dbMultsigTransaction.setSender(createDbAccount(castBigIntegerToLong((BigInteger)array[7])));
		dbMultsigTransaction.setSenderProof((byte[])array[8]);
		dbMultsigTransaction.setBlkIndex((Integer)array[9]);
		dbMultsigTransaction.setOrderId((Integer)array[10]);
		dbMultsigTransaction.setReferencedTransaction(castBigIntegerToLong((BigInteger)array[11]));
		dbMultsigTransaction.setTransferTransaction(this.multisigDbTransfers.get(castBigIntegerToLong((BigInteger)array[12])));
		dbMultsigTransaction.setImportanceTransferTransaction(this.multisigDbImportanceTransfers.get(castBigIntegerToLong((BigInteger)array[13])));
		dbMultsigTransaction.setMultisigAggregateModificationTransaction(this.multisigDbModificationTransactions.get(castBigIntegerToLong((BigInteger)array[14])));
		dbMultsigTransaction.setMultisigSignatureTransactions(new HashSet<>());

		return dbMultsigTransaction;
	}

	private DbMultisigSignatureTransaction mapToDbMulsigSignature(
			final DbMultisigTransaction dbMultisigTransaction,
			final Object[] array) {
		final DbMultisigSignatureTransaction dbMulsigSignature = new DbMultisigSignatureTransaction();
		dbMulsigSignature.setMultisigTransaction(dbMultisigTransaction);
		dbMulsigSignature.setId(castBigIntegerToLong((BigInteger)array[16]));
		dbMulsigSignature.setTransferHash(new Hash((byte[])array[17]));
		dbMulsigSignature.setVersion((Integer)array[18]);
		dbMulsigSignature.setFee(castBigIntegerToLong((BigInteger)array[19]));
		dbMulsigSignature.setTimeStamp((Integer)array[20]);
		dbMulsigSignature.setDeadline((Integer)array[21]);
		dbMulsigSignature.setSender(createDbAccount(castBigIntegerToLong((BigInteger)array[22])));
		dbMulsigSignature.setSenderProof((byte[])array[23]);

		return dbMulsigSignature;
	}

	private HashMap<Long, DbAccount> getAccountMap(final HashSet<Long> accountIds) {
		this.actionStarts();
		final Query query = this.getCurrentSession()
				.createSQLQuery("SELECT a.* FROM accounts a WHERE a.id in (:ids)")
				.addEntity(DbAccount.class)
				.setParameterList("ids", accountIds.stream().collect(Collectors.toList()));
		final List<DbAccount> accounts = listAndCast(query);
		final HashMap<Long, DbAccount> accountMap = new HashMap<>();
		accounts.stream().forEach(a -> accountMap.put(a.getId(), a));
		this.actionEnds("getAccountMap ");
		return accountMap;
	}

	private DbAccount createDbAccount(final Long id) {
		if (null == id) {
			return null;
		}

		final DbAccount dbAccount = new DbAccount();
		dbAccount.setId(id);
		return dbAccount;
	}

	private DbBlock createDbBlock(final Long id) {
		final DbBlock dbBlock = new DbBlock();
		dbBlock.setId(id);
		return dbBlock;
	}

	private Long castBigIntegerToLong(final BigInteger value) {
		return null == value ? null : value.longValue();
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> listAndCast(final Query q) {
		return q.list();
	}

	private String createColumnList(final String prefix, final int postfix, String[] columns) {
		return StringUtils.join(
				Arrays.stream(columns)
						.map(col -> String.format("%s.%s as %s%d", prefix, col, col, postfix))
						.collect(Collectors.toList()), ", ");
	}

	private <TDbModel extends AbstractTransfer> void extractMultisigTransfers(
			final List<TDbModel> allTransfers,
			final HashMap<Long, TDbModel> multisigTransfers) {
		allTransfers.stream()
				.filter(t -> null == t.getSenderProof())
				.forEach(t -> multisigTransfers.put(t.getId(), t));
	}

	private HashSet<Long> collectAccountIds(
			final List<DbBlock> dbBlocks,
			final List<DbTransferTransaction> allDbTransfers,
			final List<DbImportanceTransferTransaction> allDbImportanceTransfers,
			final List<DbMultisigAggregateModificationTransaction> allDbModificationTransactions,
			final List<DbMultisigTransaction> allDbMultisigTransactions) {
		final HashSet<Long> accountIds = new HashSet<>();
		dbBlocks.stream().forEach(b -> {
			accountIds.add(b.getHarvester().getId());
			if (null != b.getLessor()) {
				accountIds.add(b.getLessor().getId());
			}
		});
		allDbTransfers.stream().forEach(t -> {
			accountIds.add(t.getSender().getId());
			accountIds.add(t.getRecipient().getId());
		});
		allDbImportanceTransfers.stream().forEach(t -> {
			accountIds.add(t.getSender().getId());
			accountIds.add(t.getRemote().getId());
		});
		allDbModificationTransactions.stream().forEach(t -> {
			accountIds.add(t.getSender().getId());
			t.getOtherAccounts().stream().forEach(a -> accountIds.add(a.getId()));
		});
		allDbMultisigTransactions.stream().forEach(t -> {
			accountIds.add(t.getSender().getId());
			t.getMultisigSignatureTransactions().stream().forEach(s -> accountIds.add(s.getSender().getId()));
		});

		return accountIds;
	}

	private void updateAccounts(
			final List<DbBlock> dbBlocks,
			final List<DbTransferTransaction> allDbTransfers,
			final List<DbImportanceTransferTransaction> allDbImportanceTransfers,
			final List<DbMultisigAggregateModificationTransaction> allDbModificationTransactions,
			final List<DbMultisigTransaction> allDbMultisigTransactions,
			final HashMap<Long, DbAccount> accountMap) {
		dbBlocks.stream().forEach(b -> {
			b.setHarvester(accountMap.get(b.getHarvester().getId()));
			if (null != b.getLessor()) {
				b.setLessor(accountMap.get(b.getLessor().getId()));
			}
		});
		allDbTransfers.stream().forEach(t -> {
			t.setSender(accountMap.get(t.getSender().getId()));
			t.setRecipient(accountMap.get(t.getRecipient().getId()));
		});
		allDbImportanceTransfers.stream().forEach(t -> {
			t.setSender(accountMap.get(t.getSender().getId()));
			t.setRemote(accountMap.get(t.getRemote().getId()));
		});
		allDbModificationTransactions.stream().forEach(t -> {
			t.setSender(accountMap.get(t.getSender().getId()));
			t.getMultisigModifications().stream().forEach(m -> m.setCosignatory(accountMap.get(m.getCosignatory().getId())));
		});
		allDbMultisigTransactions.stream().forEach(t -> {
			t.setSender(accountMap.get(t.getSender().getId()));
			t.getMultisigSignatureTransactions().stream().forEach(s -> s.setSender(accountMap.get(s.getSender().getId())));
		});
	}

	private <TDbModel extends AbstractBlockTransfer> void addTransactions(
			final List<TDbModel> transactions,
			BiConsumer<DbBlock, TDbModel> transactionAdder) {
		transactions.stream().forEach(t -> {
			if (null != t.getSenderProof()) {
				transactionAdder.accept(this.dbBlockMap.get(t.getBlock().getId()), t);
			}
		});
	}

	// TODO 20150213: J-B i guess we can remove these?
	private void actionStarts() {
		this.start = System.currentTimeMillis();
	}

	private void actionEnds(final String prefix) {
		final long stop = System.currentTimeMillis();
		//LOGGER.info(String.format("%s needed %dms.", prefix, stop - this.start));
	}
}
