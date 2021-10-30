package org.nem.nis.dao;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.*;
import org.nem.core.model.TransactionTypes;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dao.mappers.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Class that loads blocks from the database. <br>
 * This class is used as an implementation detail of BlockDao and is tested mainly through those tests.
 */
@SuppressWarnings("rawtypes")
public class BlockLoader {
	private static final int NUM_MULTISIG_COLUMNS = 17;
	private static final String[] MULTISIG_SIGNATURES_COLUMNS = {
			"multisigtransactionid", "id", "transferhash", "version", "fee", "timestamp", "deadline", "senderid", "senderproof"
	};
	private static final String[] MULTISIG_COSIGNATORIES_MODIFICATIONS_COLUMNS = {
			"multisigsignermodificationid", "id", "cosignatoryid", "modificationtype"
	};
	private static final String[] MULTISIG_MIN_COSIGNATORIES_MODIFICATIONS_COLUMNS = {
			"id", "relativeChange"
	};
	private static final String[] NAMESPACE_COLUMNS = {
			"id", "fullName", "ownerId", "height", "level"
	};
	private static final String[] MOSAIC_DEFINITION_COLUMNS = {
			"id", "creatorid", "name", "description", "namespaceid", "feeType", "feeRecipientId", "feeDbMosaicId", "feeQuantity"
	};
	private static final String[] TRANSFERRED_MOSAICS_COLUMNS = {
			"id", "dbMosaicId", "quantity"
	};

	private final Session session;
	private final IMapper mapper;
	private final List<DbBlock> dbBlocks = new ArrayList<>();
	private final List<DbTransferTransaction> dbTransfers = new ArrayList<>();
	private final List<DbImportanceTransferTransaction> dbImportanceTransfers = new ArrayList<>();
	private final List<DbMultisigAggregateModificationTransaction> dbModificationTransactions = new ArrayList<>();
	private final List<DbProvisionNamespaceTransaction> dbProvisionNamespaceTransactions = new ArrayList<>();
	private final List<DbMosaicDefinitionCreationTransaction> dbMosaicDefinitionCreationTransactions = new ArrayList<>();
	private final List<DbMosaicSupplyChangeTransaction> dbMosaicSupplyChangeTransactions = new ArrayList<>();
	private final List<DbMultisigTransaction> dbMultisigTransactions = new ArrayList<>();
	private final HashMap<Long, DbBlock> dbBlockMap = new HashMap<>();
	private final MultisigTransferMap multisigTransferMap = new MultisigTransferMap();

	/**
	 * Creates a new block analyzer.
	 *
	 * @param session The session.
	 */
	public BlockLoader(final Session session) {
		this(session, null);
	}

	private BlockLoader(final Session session, final IMapper mapper) {
		this.session = session;
		this.mapper = null == mapper ? this.createDefaultMapper() : mapper;
	}

	private IMapper createDefaultMapper() {
		final MappingRepository mapper = new MappingRepository();
		mapper.addMapping(Long.class, DbAccount.class, new AccountRawToDbModelMapping());
		mapper.addMapping(Object[].class, DbBlock.class, new BlockRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbImportanceTransferTransaction.class, new ImportanceTransferRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigAggregateModificationTransaction.class,
				new MultisigAggregateModificationRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigModification.class, new MultisigModificationRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigMinCosignatoriesModification.class,
				new MultisigMinCosignatoriesModificationRawToDbModelMapping());
		mapper.addMapping(Object[].class, DbMultisigSignatureTransaction.class, new MultisigSignatureRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMultisigTransaction.class,
				new MultisigTransactionRawToDbModelMapping(mapper, this.multisigTransferMap));
		mapper.addMapping(Object[].class, DbTransferTransaction.class, new TransferRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbProvisionNamespaceTransaction.class, new ProvisionNamespaceRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbNamespace.class, new NamespaceRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMosaicDefinitionCreationTransaction.class,
				new MosaicDefinitionCreationRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMosaicDefinition.class, new MosaicDefinitionRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMosaicProperty.class, new MosaicPropertyRawToDbModelMapping());
		mapper.addMapping(Object[].class, DbMosaicSupplyChangeTransaction.class, new MosaicSupplyChangeRawToDbModelMapping(mapper));
		mapper.addMapping(Object[].class, DbMosaic.class, new MosaicRawToDbModelMapping());
		return mapper;
	}

	/**
	 * Loads blocks from the database with fromHeight &lt;= height &lt;= toHeight.
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
		this.extractMultisigTransfers(this.dbTransfers, TransactionTypes.TRANSFER);
		this.dbImportanceTransfers.addAll(this.getDbImportanceTransfers(minBlockId, maxBlockId));
		this.extractMultisigTransfers(this.dbImportanceTransfers, TransactionTypes.IMPORTANCE_TRANSFER);
		this.dbModificationTransactions.addAll(this.getDbModificationTransactions(minBlockId, maxBlockId));
		this.extractMultisigTransfers(this.dbModificationTransactions, TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION);
		this.dbProvisionNamespaceTransactions.addAll(this.getDbProvisionNamespaceTransactions(minBlockId, maxBlockId));
		this.extractMultisigTransfers(this.dbProvisionNamespaceTransactions, TransactionTypes.PROVISION_NAMESPACE);
		this.dbMosaicDefinitionCreationTransactions.addAll(this.getDbMosaicDefinitionCreationTransactions(minBlockId, maxBlockId));
		this.extractMultisigTransfers(this.dbMosaicDefinitionCreationTransactions, TransactionTypes.MOSAIC_DEFINITION_CREATION);
		this.dbMosaicSupplyChangeTransactions.addAll(this.getDbMosaicSupplyChangeTransactions(minBlockId, maxBlockId));
		this.extractMultisigTransfers(this.dbMosaicSupplyChangeTransactions, TransactionTypes.MOSAIC_SUPPLY_CHANGE);
		this.dbMultisigTransactions.addAll(this.getDbMultisigTransactions(minBlockId, maxBlockId));
	}

	private void addTransactionsToBlocks() {
		this.addTransactions(this.dbTransfers, DbBlock::addTransferTransaction);
		this.addTransactions(this.dbImportanceTransfers, DbBlock::addImportanceTransferTransaction);
		this.addTransactions(this.dbModificationTransactions, DbBlock::addMultisigAggregateModificationTransaction);
		this.addTransactions(this.dbMultisigTransactions, DbBlock::addMultisigTransaction);
		this.addTransactions(this.dbProvisionNamespaceTransactions, DbBlock::addProvisionNamespaceTransaction);
		this.addTransactions(this.dbMosaicDefinitionCreationTransactions, DbBlock::addMosaicDefinitionCreationTransaction);
		this.addTransactions(this.dbMosaicSupplyChangeTransactions, DbBlock::addMosaicSupplyChangeTransaction);
	}

	private List<DbBlock> getDbBlocks(final BlockHeight fromHeight, final BlockHeight toHeight) {
		final Query query = this.session
				.createSQLQuery(
						"SELECT b.* FROM BLOCKS b WHERE height >= :fromHeight AND height <= :toHeight ORDER BY height ASC LIMIT :limit")
				.setParameter("fromHeight", fromHeight.getRaw()) // preserve-newline
				.setParameter("toHeight", toHeight.getRaw()) // preserve-newline
				.setParameter("limit", toHeight.getRaw() - fromHeight.getRaw() + 1);
		return this.executeAndMapAll(query, DbBlock.class);
	}

	private List<DbBlock> getDbBlockById(final long blockId) {
		final Query query = this.session.createSQLQuery("SELECT b.* FROM BLOCKS b WHERE id = :blockId") // preserve-newline
				.setParameter("blockId", blockId);
		return this.executeAndMapAll(query, DbBlock.class);
	}

	private List<DbTransferTransaction> getDbTransfers(final long minBlockId, final long maxBlockId) {
		final String transferredMosaicsColumns = this.createColumnList("tm", 1, TRANSFERRED_MOSAICS_COLUMNS);

		final String queryString = "SELECT t.*, " + transferredMosaicsColumns + " FROM transfers t "
				+ "LEFT OUTER JOIN transferredMosaics tm ON tm.transferId = t.id "
				+ "WHERE blockid > :minBlockId AND blockid < :maxBlockId " // preserve-newline
				+ "ORDER BY blockid ASC";
		final Query query = this.session.createSQLQuery(queryString) // preserve-newline
				.setParameter("minBlockId", minBlockId) // preserve-newline
				.setParameter("maxBlockId", maxBlockId);
		final List<Object[]> objects = HibernateUtils.listAndCast(query);
		return this.mapToTransferTransactions(objects);
	}

	private List<DbTransferTransaction> mapToTransferTransactions(final List<Object[]> arrays) {
		if (arrays.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbTransferTransaction> transactions = new ArrayList<>();
		DbTransferTransaction dbTransferTransaction = null;
		long curTxId = 0L;
		for (final Object[] array : arrays) {
			final Long txId = RawMapperUtils.castToLong(array[1]);
			if (curTxId != txId) {
				curTxId = txId;
				dbTransferTransaction = this.mapper.map(array, DbTransferTransaction.class);
				transactions.add(dbTransferTransaction);
			}

			assert null != dbTransferTransaction;

			// array[15] = transferred mosaics row id if available
			if (null != array[15]) {
				dbTransferTransaction.getMosaics().add(this.mapper.map(Arrays.copyOfRange(array, 15, array.length), DbMosaic.class));
			}
		}

		return transactions;
	}

	private List<DbImportanceTransferTransaction> getDbImportanceTransfers(final long minBlockId, final long maxBlockId) {
		final String queryString = "SELECT t.* FROM importancetransfers t " // preserve-newline
				+ "WHERE blockid > :minBlockId AND blockid < :maxBlockId " // preserve-newline
				+ "ORDER BY blockid ASC";
		final Query query = this.session.createSQLQuery(queryString) // preserve-newline
				.setParameter("minBlockId", minBlockId) // preserve-newline
				.setParameter("maxBlockId", maxBlockId);
		return this.executeAndMapAll(query, DbImportanceTransferTransaction.class);
	}

	private List<DbMultisigAggregateModificationTransaction> getDbModificationTransactions(final long minBlockId, final long maxBlockId) {
		final String cosignatoryModificationColumnList = this.createColumnList("mcm", 1, MULTISIG_COSIGNATORIES_MODIFICATIONS_COLUMNS);
		final String minCosignatoryModificationColumnList = this.createColumnList("mmcm", 2,
				MULTISIG_MIN_COSIGNATORIES_MODIFICATIONS_COLUMNS);

		final String queryString = "SELECT msm.*, " + cosignatoryModificationColumnList + ", " + minCosignatoryModificationColumnList
				+ " FROM multisigsignermodifications msm "
				+ "LEFT OUTER JOIN multisigModifications mcm ON mcm.multisigsignermodificationid = msm.id "
				+ "LEFT OUTER JOIN minCosignatoriesModifications mmcm ON msm.minCosignatoriesModificationId = mmcm.id "
				+ "WHERE msm.blockid > :minBlockId AND msm.blockid < :maxBlockId " // preserve-newline
				+ "ORDER BY msm.blockid ASC";
		final Query query = this.session.createSQLQuery(queryString) // preserve-newline
				.setParameter("minBlockId", minBlockId) // preserve-newline
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
			final Long txId = RawMapperUtils.castToLong(array[12]); // 12 is mm.multisigSignerModificationId
			if (null == txId || curTxId != txId) {
				dbModificationTransaction = this.mapToDbModificationTransaction(array);
				dbModificationTransaction.setMultisigMinCosignatoriesModification(this.mapToDbMinCosignatoriesModification(array));
				transactions.add(dbModificationTransaction);
				if (null == txId) { // no cosignatory modifications
					continue;
				}

				curTxId = txId;
			}

			assert null != dbModificationTransaction;
			dbModificationTransaction.getMultisigModifications().add(this.mapToDbCosignatoryModification(dbModificationTransaction, array));
		}

		return transactions;
	}

	private DbMultisigAggregateModificationTransaction mapToDbModificationTransaction(final Object[] array) {
		return this.mapper.map(array, DbMultisigAggregateModificationTransaction.class);
	}

	private DbMultisigModification mapToDbCosignatoryModification(
			final DbMultisigAggregateModificationTransaction dbModificationTransaction, final Object[] array) {
		final DbMultisigModification dbModification = this.mapper.map(array, DbMultisigModification.class);
		dbModification.setMultisigAggregateModificationTransaction(dbModificationTransaction);
		return dbModification;
	}

	private DbMultisigMinCosignatoriesModification mapToDbMinCosignatoriesModification(final Object[] array) {
		return this.mapper.map(array, DbMultisigMinCosignatoriesModification.class);
	}

	private List<DbMultisigTransaction> getDbMultisigTransactions(final long minBlockId, final long maxBlockId) {
		final String columnList = this.createColumnList("ms", 1, MULTISIG_SIGNATURES_COLUMNS);
		final String queryString = "SELECT mt.*, " + columnList + " FROM MULTISIGTRANSACTIONS mt "
				+ "LEFT OUTER JOIN multisigsignatures ms on ms.multisigtransactionid = mt.id "
				+ "WHERE mt.blockid > :minBlockId and mt.blockid < :maxBlockId " // preserve-newline
				+ "ORDER BY mt.blockid ASC";
		final Query query = this.session.createSQLQuery(queryString).setParameter("minBlockId", minBlockId) // preserve-newline
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
			final Long txid = RawMapperUtils.castToLong(array[NUM_MULTISIG_COLUMNS]);
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

	private DbMultisigSignatureTransaction mapToDbMultisigSignature(final DbMultisigTransaction dbMultisigTransaction,
			final Object[] array) {
		final DbMultisigSignatureTransaction dbMultisigSignature = this.mapper
				.map(Arrays.copyOfRange(array, NUM_MULTISIG_COLUMNS, array.length), DbMultisigSignatureTransaction.class);
		dbMultisigSignature.setMultisigTransaction(dbMultisigTransaction);
		return dbMultisigSignature;
	}

	private List<DbProvisionNamespaceTransaction> getDbProvisionNamespaceTransactions(final long minBlockId, final long maxBlockId) {
		final String columnList = this.createColumnList("n", 1, NAMESPACE_COLUMNS);
		final String queryString = "SELECT np.*, " + columnList + " FROM namespaceProvisions np "
				+ "LEFT OUTER JOIN namespaces n on np.namespaceId = n.id " // preserve-newline
				+ "WHERE np.blockid > :minBlockId AND np.blockid < :maxBlockId " // preserve-newline
				+ "ORDER BY np.blockid ASC";
		final Query query = this.session.createSQLQuery(queryString) // preserve-newline
				.setParameter("minBlockId", minBlockId) // preserve-newline
				.setParameter("maxBlockId", maxBlockId);
		return this.executeAndMapAll(query, DbProvisionNamespaceTransaction.class);
	}

	private List<DbMosaicDefinitionCreationTransaction> getDbMosaicDefinitionCreationTransactions(final long minBlockId,
			final long maxBlockId) {
		final String columnList = this.createColumnList("m", 1, MOSAIC_DEFINITION_COLUMNS);
		final String queryString = "SELECT t.*, " + columnList + " FROM mosaicDefinitionCreationTransactions t "
				+ "LEFT OUTER JOIN mosaicdefinitions m on t.mosaicDefinitionId = m.id "
				+ "WHERE t.blockid > :minBlockId AND t.blockid < :maxBlockId " // preserve-newline
				+ "ORDER BY t.blockid ASC";
		final Query query = this.session.createSQLQuery(queryString) // preserve-newline
				.setParameter("minBlockId", minBlockId) // preserve-newline
				.setParameter("maxBlockId", maxBlockId);
		final List<DbMosaicDefinitionCreationTransaction> transactions = this.executeAndMapAll(query,
				DbMosaicDefinitionCreationTransaction.class);
		this.insertMosaicDefinitionProperties(transactions);
		return transactions;
	}

	private void insertMosaicDefinitionProperties(final Collection<DbMosaicDefinitionCreationTransaction> transactions) {
		if (transactions.isEmpty()) {
			return;
		}

		final HashMap<Long, DbMosaicDefinition> map = new HashMap<>(transactions.size());
		transactions.stream().map(DbMosaicDefinitionCreationTransaction::getMosaicDefinition).forEach(m -> map.put(m.getId(), m));
		final String queryString = "SELECT mp.* FROM mosaicproperties mp " // preserve-newline
				+ "WHERE mp.mosaicDefinitionId in (:ids) " // preserve-newline
				+ "ORDER BY mp.mosaicDefinitionId ASC";
		final Query query = this.session.createSQLQuery(queryString) // preserve-newline
				.setParameterList("ids", map.keySet());
		final List<Object[]> arrays = HibernateUtils.listAndCast(query);
		for (final Object[] array : arrays) {
			// array[0] = mosaic id
			final Long mosaicId = RawMapperUtils.castToLong(array[0]);
			final DbMosaicDefinition dbMosaicDefinition = map.get(mosaicId);
			assert null != dbMosaicDefinition;

			final DbMosaicProperty property = this.mapper.map(array, DbMosaicProperty.class);
			property.setMosaicDefinition(dbMosaicDefinition);
			dbMosaicDefinition.getProperties().add(property);
		}
	}

	private List<DbMosaicSupplyChangeTransaction> getDbMosaicSupplyChangeTransactions(final long minBlockId, final long maxBlockId) {
		final String queryString = "SELECT t.* FROM mosaicsupplychanges t " // preserve-newline
				+ "WHERE blockid > :minBlockId AND blockid < :maxBlockId " // preserve-newline
				+ "ORDER BY blockid ASC";
		final Query query = this.session.createSQLQuery(queryString) // preserve-newline
				.setParameter("minBlockId", minBlockId) // preserve-newline
				.setParameter("maxBlockId", maxBlockId);
		return this.executeAndMapAll(query, DbMosaicSupplyChangeTransaction.class);
	}

	private <T> List<T> executeAndMapAll(final Query query, final Class<T> targetClass) {
		final List<Object[]> objects = HibernateUtils.listAndCast(query);
		return objects.stream().map(raw -> this.mapper.map(raw, targetClass)).collect(Collectors.toList());
	}

	private HashMap<Long, DbAccount> getAccounts(final HashSet<DbAccount> accounts) {
		final String ids = accounts.stream().map(a -> a.getId().toString()).collect(Collectors.joining(","));
		final String sql = String.format("SELECT a.* FROM accounts a WHERE a.id in (%s)", ids);
		final Query query = this.session.createSQLQuery(sql) // preserve-newline
				.addEntity(DbAccount.class);
		final List<DbAccount> realAccounts = HibernateUtils.listAndCast(query);
		final HashMap<Long, DbAccount> accountMap = new HashMap<>();
		realAccounts.stream().forEach(a -> accountMap.put(a.getId(), a));
		return accountMap;
	}

	private String createColumnList(final String prefix, final int postfix, final String[] columns) {
		return StringUtils.join(
				Arrays.stream(columns).map(col -> String.format("%s.%s as %s%d", prefix, col, col, postfix)).collect(Collectors.toList()),
				", ");
	}

	private <TDbModel extends AbstractBlockTransfer> void extractMultisigTransfers(final Collection<TDbModel> allTransfers,
			final int transferType) {
		final MultisigTransferMap.Entry entry = this.multisigTransferMap.getEntry(transferType);
		allTransfers.stream().filter(t -> null == t.getSenderProof()).forEach(entry::add);
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

	private static <TDbModel extends AbstractBlockTransfer> void collectAccountsFromTransaction(final DbBlock block,
			final TransactionRegistry.Entry<TDbModel, ?> theEntry, final Set<DbAccount> accounts) {
		final List<TDbModel> transactions = theEntry.getFromBlock.apply(block);
		transactions.stream().forEach(t -> {
			accounts.add(t.getSender());
			final DbAccount recipient = theEntry.getRecipient.apply(t);
			if (null != recipient) {
				accounts.add(recipient);
			}
			theEntry.getOtherAccounts.apply(t).stream().forEach(accounts::add);

			collectAccountsFromInnerTransaction(theEntry.getInnerTransaction.apply(t), accounts);
		});
	}

	private static <TDbModel extends AbstractBlockTransfer> void collectAccountsFromInnerTransaction(final TDbModel innerTransaction,
			final Set<DbAccount> accounts) {
		if (null == innerTransaction) {
			return;
		}

		accounts.add(innerTransaction.getSender());

		@SuppressWarnings("unchecked")
		final TransactionRegistry.Entry<TDbModel, ?> innerEntry = TransactionRegistry
				.findByDbModelClass((Class<TDbModel>) innerTransaction.getClass());

		assert null != innerEntry;
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

	private static <TDbModel extends AbstractBlockTransfer> void addInnerAccounts(final TDbModel innerTransaction,
			final HashMap<Long, DbAccount> accountMap) {
		if (null == innerTransaction) {
			return;
		}

		final TransactionRegistry.Entry<AbstractBlockTransfer, ?> innerEntry = TransactionRegistry
				.findByDbModelClass(innerTransaction.getClass());
		addAccounts(innerTransaction, innerEntry, accountMap);
	}

	private static <TDbModel extends AbstractBlockTransfer> void addAccounts(final TDbModel transaction,
			final TransactionRegistry.Entry<TDbModel, ?> theEntry, final HashMap<Long, DbAccount> accountMap) {
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

	private <TDbModel extends AbstractBlockTransfer> void addTransactions(final List<TDbModel> transactions,
			final BiConsumer<DbBlock, TDbModel> transactionAdder) {
		transactions.stream().forEach(t -> {
			if (null != t.getSenderProof()) {
				transactionAdder.accept(this.dbBlockMap.get(t.getBlock().getId()), t);
			}
		});
	}
}
