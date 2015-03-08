package org.nem.nis.dao;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dao.mappers.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

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
			"multisigtransactionid", "id", "transferhash", "version", "fee", "timestamp", "deadline", "senderid", "senderproof" };
	private final static String[] multisigModificationsColumns = {
			"multisigsignermodificationid", "id", "cosignatoryid", "modificationtype" };

	private final SessionFactory sessionFactory;
	private final IMapper mapper;
	private final List<DbBlock> dbBlocks = new ArrayList<>();
	private final List<DbTransferTransaction> dbTransfers = new ArrayList<>();
	private final List<DbImportanceTransferTransaction> dbImportanceTransfers = new ArrayList<>();
	private final List<DbMultisigAggregateModificationTransaction> dbbModificationTransactions = new ArrayList<>();
	private final List<DbMultisigTransaction> dbMultisigTransactions = new ArrayList<>();
	private final HashMap<Long, DbBlock> dbBlockMap = new HashMap<>();
	private final HashMap<Long, DbTransferTransaction> multisigDbTransferMap = new HashMap<>();
	private final HashMap<Long, DbImportanceTransferTransaction> multisigDbImportanceTransferMap = new HashMap<>();
	private final HashMap<Long, DbMultisigAggregateModificationTransaction> multisigDbModificationTransactionMap = new HashMap<>();

	/**
	 * Creates a new block analyzer.
	 *
	 * @param sessionFactory The session factory.
	 */
	public BlockLoader(final SessionFactory sessionFactory) {
		this(sessionFactory, null);
	}

	/**
	 * Creates a new block analyzer.
	 *
	 * @param sessionFactory The session factory.
	 * @param mapper The mapper.
	 */
	public BlockLoader(final SessionFactory sessionFactory, final IMapper mapper) {
		this.sessionFactory = sessionFactory;
		this.mapper = null == mapper ? this.createDefaultMapper() : mapper;
	}

	private IMapper createDefaultMapper() {
		final MappingRepository mapper = new MappingRepository();
		mapper.addMapping(Long.class, DbAccount.class, new AccountRawToDbModelMapping());
		mapper.addMapping(Object[].class, DbBlock.class, new BlockRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbImportanceTransferTransaction.class, new ImportanceTransferRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigAggregateModificationTransaction.class, new MultisigAggregateModificationRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigModification.class, new MultisigModificationRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigSignatureTransaction.class, new MultisigSignatureRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigTransaction.class, new MultisigTransactionRawToDbModelMapping(
				mapper,
				id -> null == id ? null : this.multisigDbTransferMap.get(id),
				id -> null == id ? null : this.multisigDbImportanceTransferMap.get(id),
				id -> null == id ? null : this.multisigDbModificationTransactionMap.get(id)));
		mapper.addMapping(Object[].class, DbTransferTransaction.class, new TransferRawToDbModelMapping(mapper));
		return mapper;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	/**
	 * Loads blocks from the database with fromHeight <= height <= toHeight.
	 *
	 * @param fromHeight The height from which on to pull blocks.
	 * @param toHeight The height up to which blocks should be pulled.
	 * @return The list of db blocks.
	 */
	public List<DbBlock> loadBlocks(final BlockHeight fromHeight, final BlockHeight toHeight) {
		this.dbBlocks.addAll(this.getDbBlocks(fromHeight, toHeight));
		if (this.dbBlocks.isEmpty()) {
			return new ArrayList<>();
		}
		this.retrieveSubTables();
		return this.dbBlocks;
	}

	/**
	 * Gets a single block by its id.
	 *
	 * @param blockId The block id.
	 * @return The db block.
	 */
	public DbBlock getBlockById(final Long blockId) {
		final List<DbBlock> blockList = this.getDbBlockById(blockId);
		if (blockList.isEmpty()) {
			return null;
		}

		this.dbBlocks.add(blockList.get(0));
		this.retrieveSubTables();
		return this.dbBlocks.get(0);
	}

	private void retrieveSubTables() {
		this.dbBlocks.stream().forEach(b -> this.dbBlockMap.put(b.getId(), b));
		final long minBlockId = this.dbBlocks.get(0).getId() - 1;
		final long maxBlockId = this.dbBlocks.get(this.dbBlocks.size() - 1).getId() + 1;
		this.retrieveTransactions(minBlockId, maxBlockId);
		this.addTransactionsToBlocks();
		final HashSet<DbAccount> accounts = this.collectAccounts();
		final HashMap<Long, DbAccount> accountMap = this.getAccounts(accounts);
		this.updateAccounts(accountMap);
	}

	private void retrieveTransactions(final long minBlockId, final long maxBlockId) {
		this.dbTransfers.addAll(this.getDbTransfers(minBlockId, maxBlockId));
		this.extractMultisigTransfers(this.dbTransfers, this.multisigDbTransferMap);
		this.dbImportanceTransfers.addAll(this.getDbImportanceTransfers(minBlockId, maxBlockId));
		this.extractMultisigTransfers(this.dbImportanceTransfers, this.multisigDbImportanceTransferMap);
		this.dbbModificationTransactions.addAll(this.getDbModificationTransactions(minBlockId, maxBlockId));
		this.extractMultisigTransfers(this.dbbModificationTransactions, this.multisigDbModificationTransactionMap);
		this.dbMultisigTransactions.addAll(this.getDbMultisigTransactions(minBlockId, maxBlockId));
	}

	private void addTransactionsToBlocks() {
		this.addTransactions(this.dbTransfers, DbBlock::addTransferTransaction);
		this.addTransactions(this.dbImportanceTransfers, DbBlock::addImportanceTransferTransaction);
		this.addTransactions(this.dbbModificationTransactions, DbBlock::addMultisigAggregateModificationTransaction);
		this.addTransactions(this.dbMultisigTransactions, DbBlock::addMultisigTransaction);
	}

	private List<DbBlock> getDbBlocks(final BlockHeight fromHeight, final BlockHeight toHeight) {
		final Query query = this.getCurrentSession()
				.createSQLQuery("SELECT b.* FROM BLOCKS b WHERE height >= :fromHeight AND height <= :toHeight ORDER BY height ASC LIMIT :limit")
				.setParameter("fromHeight", fromHeight.getRaw())
				.setParameter("toHeight", toHeight.getRaw())
				.setParameter("limit", toHeight.getRaw() - fromHeight.getRaw() + 1);
		return this.executeAndMapAll(query, DbBlock.class);
	}

	private List<DbBlock> getDbBlockById(final long blockId) {
		final Query query = this.getCurrentSession()
				.createSQLQuery("SELECT b.* FROM BLOCKS b WHERE id = :blockId")
				.setParameter("blockId", blockId);
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
		final List<Object[]> objects = HibernateUtils.listAndCast(query);
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
		final List<Object[]> objects = HibernateUtils.listAndCast(query);
		return this.mapToDbModificationTransactions(objects);
	}

	private List<DbMultisigAggregateModificationTransaction> mapToDbModificationTransactions(final List<Object[]> arrays) {
		if (arrays.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigAggregateModificationTransaction> transactions = new ArrayList<>();
		DbMultisigAggregateModificationTransaction dbModificationTransaction = null;
		long curTxId = 0L;
		for (final Object[] array : arrays) {
			final long txid = RawMapperUtils.castToLong(array[11]); // 11 is mm.multisigSignerModificationId
			if (curTxId != txid) {
				curTxId = txid;
				dbModificationTransaction = this.mapToDbModificationTransaction(array);
				transactions.add(dbModificationTransaction);
			}

			assert null != dbModificationTransaction;
			dbModificationTransaction.getMultisigModifications().add(this.mapToDbModification(dbModificationTransaction, array));
		}

		return transactions;
	}

	private DbMultisigAggregateModificationTransaction mapToDbModificationTransaction(final Object[] array) {
		return this.mapper.map(array, DbMultisigAggregateModificationTransaction.class);
	}

	private DbMultisigModification mapToDbModification(
			final DbMultisigAggregateModificationTransaction dbModificationTransaction,
			final Object[] array) {
		final DbMultisigModification dbModification = this.mapper.map(array, DbMultisigModification.class);
		dbModification.setMultisigAggregateModificationTransaction(dbModificationTransaction);
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
		final List<Object[]> objects = HibernateUtils.listAndCast(query);
		return this.mapToDbMultisigTransactions(objects);
	}

	private List<DbMultisigTransaction> mapToDbMultisigTransactions(final List<Object[]> arrays) {
		if (arrays.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigTransaction> transactions = new ArrayList<>();
		DbMultisigTransaction dbMultisigTransaction = null;
		long curTxId = 0L;
		for (final Object[] array : arrays) {
			final Long txid = RawMapperUtils.castToLong(array[14]);
			if (null == txid) {
				// no cosignatories
				dbMultisigTransaction = this.mapToDbMultisigTransaction(array);
				transactions.add(dbMultisigTransaction);
				continue;
			}

			if (curTxId != txid) {
				curTxId = txid;
				dbMultisigTransaction = this.mapToDbMultisigTransaction(array);
				transactions.add(dbMultisigTransaction);
			}

			assert null != dbMultisigTransaction;
			dbMultisigTransaction.getMultisigSignatureTransactions().add(this.mapToDbMultisigSignature(dbMultisigTransaction, array));
		}

		return transactions;
	}

	private DbMultisigTransaction mapToDbMultisigTransaction(final Object[] array) {
		return this.mapper.map(array, DbMultisigTransaction.class);
	}

	private DbMultisigSignatureTransaction mapToDbMultisigSignature(
			final DbMultisigTransaction dbMultisigTransaction,
			final Object[] array) {
		final DbMultisigSignatureTransaction dbMultisigSignature = this.mapper.map(
				Arrays.copyOfRange(array, 14, array.length),
				DbMultisigSignatureTransaction.class);
		dbMultisigSignature.setMultisigTransaction(dbMultisigTransaction);
		return dbMultisigSignature;
	}

	private HashMap<Long, DbAccount> getAccounts(final HashSet<DbAccount> accounts) {
		final Query query = this.getCurrentSession()
				.createSQLQuery("SELECT a.* FROM accounts a WHERE a.id in (:ids)")
				.addEntity(DbAccount.class)
				.setParameterList("ids", accounts.stream().map(DbAccount::getId).collect(Collectors.toList()));
		final List<DbAccount> realAccounts = HibernateUtils.listAndCast(query);
		final HashMap<Long, DbAccount> accountMap = new HashMap<>();
		realAccounts.stream().forEach(a -> accountMap.put(a.getId(), a));
		return accountMap;
	}

	private String createColumnList(final String prefix, final int postfix, final String[] columns) {
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

	private HashSet<DbAccount> collectAccounts() {
		final HashSet<DbAccount> accounts = new HashSet<>();
		this.dbBlocks.stream().forEach(b -> {
			accounts.add(b.getHarvester());
			if (null != b.getLessor()) {
				accounts.add(b.getLessor());
			}
			for (final TransactionRegistry.Entry<? extends AbstractBlockTransfer, ?> entry : TransactionRegistry.iterate()) {
				collectAccountsFromTransaction(b, entry, accounts);
			}
		});

		return accounts;
	}

	private static <TDbModel extends AbstractBlockTransfer> void collectAccountsFromTransaction(
			final DbBlock block,
			final TransactionRegistry.Entry<TDbModel, ?> theEntry,
			final Set<DbAccount> accounts) {
		final List<TDbModel> transactions = theEntry.getFromBlock.apply(block);
		transactions.stream().forEach(t -> {
			accounts.add(t.getSender());
			final DbAccount recipient = theEntry.getRecipient.apply(t);
			if (null != recipient) {
				accounts.add(recipient);
			}
			theEntry.getOtherAccounts.apply(t).stream().forEach(accounts::add);

			collectAccountsFromInnerTransaction(
					theEntry.getInnerTransaction.apply(t),
					accounts);
		});
	}

	private static <TDbModel extends AbstractBlockTransfer> void collectAccountsFromInnerTransaction(
			final TDbModel innerTransaction,
			final Set<DbAccount> accounts) {
		if (null == innerTransaction) {
			return;
		}

		accounts.add(innerTransaction.getSender());

		@SuppressWarnings("unchecked")
		final TransactionRegistry.Entry<TDbModel, ?> innerEntry =
				TransactionRegistry.findByDbModelClass((Class<TDbModel>)innerTransaction.getClass());

		final DbAccount innerRecipient = innerEntry.getRecipient.apply(innerTransaction);
		if (null != innerRecipient) {
			accounts.add(innerRecipient);
		}
		innerEntry.getOtherAccounts.apply(innerTransaction).stream().forEach(accounts::add);
	}

	private void updateAccounts(final HashMap<Long, DbAccount> accountMap) {
		this.dbBlocks.stream().forEach(b -> {
			addAccount(b.getHarvester(), accountMap);
			addAccount(b.getLessor(), accountMap);

			for (final TransactionRegistry.Entry<AbstractBlockTransfer, ?> entry : TransactionRegistry.iterate()) {
				entry.getFromBlock.apply(b).stream().forEach(transaction -> {
					addAccounts(transaction, entry, accountMap);
					addInnerAccounts(entry.getInnerTransaction.apply(transaction), accountMap);
				});
			}
		});
	}

	private static <TDbModel extends AbstractBlockTransfer> void addInnerAccounts(
			final TDbModel innerTransaction,
			final HashMap<Long, DbAccount> accountMap) {
		if (null == innerTransaction) {
			return;
		}

		final TransactionRegistry.Entry<AbstractBlockTransfer, ?> innerEntry =
				TransactionRegistry.findByDbModelClass(innerTransaction.getClass());
		addAccounts(innerTransaction, innerEntry, accountMap);
	}

	private static <TDbModel extends AbstractBlockTransfer> void addAccounts(
			final TDbModel transaction,
			final TransactionRegistry.Entry<TDbModel, ?> theEntry,
			final HashMap<Long, DbAccount> accountMap) {
		addAccount(transaction.getSender(), accountMap);
		addAccount(theEntry.getRecipient.apply(transaction), accountMap);
		theEntry.getOtherAccounts.apply(transaction).stream().forEach(a -> addAccount(a, accountMap));
	}

	private static void addAccount(final DbAccount dbAccount, final HashMap<Long, DbAccount> accountMap) {
		if (null == dbAccount) {
			return;
		}

		final DbAccount realAccount = accountMap.get(dbAccount.getId());
		dbAccount.setPrintableKey(realAccount.getPrintableKey());
		dbAccount.setPublicKey(realAccount.getPublicKey());
	}

	private <TDbModel extends AbstractBlockTransfer> void addTransactions(
			final List<TDbModel> transactions,
			final BiConsumer<DbBlock, TDbModel> transactionAdder) {
		transactions.stream().forEach(t -> {
			if (null != t.getSenderProof()) {
				transactionAdder.accept(this.dbBlockMap.get(t.getBlock().getId()), t);
			}
		});
	}
}
