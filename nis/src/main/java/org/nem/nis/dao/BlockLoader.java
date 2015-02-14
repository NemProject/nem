package org.nem.nis.dao;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dao.mappers.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Class that loads blocks from the database.
 * <br>
 * This class is used as an implementation detail of BlockDao and is tested mainly through those tests.
 */
public class BlockLoader {
	private final static String[] multisigSignaturesColumns = {
			"multisigtransactionid", "id", "transferhash", "version", "fee", "timestamp", "deadline", "senderid", "senderproof"	};
	private final static String[] multisigModificationsColumns = {
			"multisigsignermodificationid", "id", "cosignatoryid", "modificationtype" };

	private final SessionFactory sessionFactory;
	private final IMapper mapper;
	private final HashMap<Long, DbBlock> dbBlockMap = new HashMap<>();
	private final HashMap<Long, DbTransferTransaction> multisigDbTransfers = new HashMap<>();
	private final HashMap<Long, DbImportanceTransferTransaction> multisigDbImportanceTransfers = new HashMap<>();
	private final HashMap<Long, DbMultisigAggregateModificationTransaction> multisigDbModificationTransactions = new HashMap<>();

	/**
	 * Creates a new block analyzer.
	 *
	 * @param sessionFactory The session factory.
	 */
	public BlockLoader(final SessionFactory sessionFactory) {
		this(sessionFactory, createDefaultMapper());
	}

	/**
	 * Creates a new block analyzer.
	 *
	 * @param sessionFactory The session factory.
	 * @param mapper The mapper.
	 */
	public BlockLoader(final SessionFactory sessionFactory, final IMapper mapper) {
		this.sessionFactory = sessionFactory;
		this.mapper = mapper;
	}

	private static IMapper createDefaultMapper() {
		final MappingRepository mapper = new MappingRepository();
		mapper.addMapping(Long.class, DbAccount.class, new AccountRawToDbModelMapping());
		mapper.addMapping(Object[].class, DbBlock.class, new BlockRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbImportanceTransferTransaction.class, new ImportanceTransferRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigAggregateModificationTransaction.class, new MultisigAggregateModificationRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigModification.class, new MultisigModificationRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigSignatureTransaction.class, new MultisigSignatureRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigTransaction.class, new MultisigTransactionRawToDbModelMapping(
				mapper,
				raw -> mapper.map(raw, DbTransferTransaction.class),
				raw -> mapper.map(raw, DbImportanceTransferTransaction.class),
				raw -> mapper.map(raw, DbMultisigAggregateModificationTransaction.class)));
		mapper.addMapping(Object[].class, DbTransferTransaction.class, new TransferRawToDbModelMapping(mapper));
		return mapper;
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
		final Query query = this.getCurrentSession()
				.createSQLQuery("SELECT b.* FROM BLOCKS b WHERE height > :fromHeight AND height <= :toHeight ORDER BY height ASC LIMIT :limit")
				.setParameter("fromHeight", fromHeight.getRaw())
				.setParameter("toHeight", toHeight.getRaw())
				.setParameter("limit", toHeight.getRaw() - fromHeight.getRaw());
		return this.executeAndMapAll(query, DbBlock.class);
	}

	private List<DbTransferTransaction> getDbTransfers(
			final long minBlockId,
			final long maxBlockId) {
		final String queryString = "SELECT t.* FROM transfers t " +
				"WHERE blockid > :minBlockId AND blockid < :maxBlockId " +
				"ORDER BY blockid ASC";
		final Query query = this.getCurrentSession()
				.createSQLQuery(queryString)
				.setParameter("minBlockId", minBlockId)
				.setParameter("maxBlockId", maxBlockId);
		return this.executeAndMapAll(query, DbTransferTransaction.class);
	}

	private List<DbImportanceTransferTransaction> getDbImportanceTransfers(
			final long minBlockId,
			final long maxBlockId) {
		final String queryString = "SELECT t.* FROM importancetransfers t " +
				"WHERE blockid > :minBlockId AND blockid < :maxBlockId " +
				"ORDER BY blockid ASC";
		final Query query = this.getCurrentSession()
				.createSQLQuery(queryString)
				.setParameter("minBlockId", minBlockId)
				.setParameter("maxBlockId", maxBlockId);
		return this.executeAndMapAll(query, DbImportanceTransferTransaction.class);
	}

	private <T> List<T> executeAndMapAll(final Query query, final Class<T> targetClass) {
		final List<Object[]> objects = listAndCast(query);
		return objects.stream().map(raw -> this.mapper.map(raw, targetClass)).collect(Collectors.toList());
	}

	private List<DbMultisigAggregateModificationTransaction> getDbModificationTransactions(
			final long minBlockId,
			final long maxBlockId) {
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
		final Query query = this.getCurrentSession()
				.createSQLQuery("SELECT a.* FROM accounts a WHERE a.id in (:ids)")
				.addEntity(DbAccount.class)
				.setParameterList("ids", accountIds.stream().collect(Collectors.toList()));
		final List<DbAccount> accounts = listAndCast(query);
		final HashMap<Long, DbAccount> accountMap = new HashMap<>();
		accounts.stream().forEach(a -> accountMap.put(a.getId(), a));
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
}
