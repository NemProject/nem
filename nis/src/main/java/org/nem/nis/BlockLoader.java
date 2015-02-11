package org.nem.nis;

import org.hibernate.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.*;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class that loads blocks from the database.
 */
public class BlockLoader {
	private static final Logger LOGGER = Logger.getLogger(BlockLoader.class.getName());

	private final SessionFactory sessionFactory;

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

		final long minBlockId = dbBlocks.get(0).getId();
		final long maxBlockId = dbBlocks.get(dbBlocks.size() - 1).getId();
		final List<DbTransferTransaction> dbTransfers = this.getDbTransfers(minBlockId, maxBlockId);
		final List<DbImportanceTransferTransaction> dbImportanceTransfers = this.getDbImportanceTransfers(minBlockId, maxBlockId);
		final List<DbMultisigAggregateModificationTransaction> dbModificationTransactions = this.getDbModificationTransactions(minBlockId, maxBlockId);

		return dbBlocks;
	}

	private List<DbBlock> getDbBlocks(final BlockHeight fromHeight, final BlockHeight toHeight) {
		final Query query = this.getCurrentSession()
				.createSQLQuery("SELECT b.* FROM BLOCKS b WHERE height > :fromHeight AND height <= :toHeight ORDER BY height ASC LIMIT :limit")
				.setParameter("fromHeight", fromHeight.getRaw())
				.setParameter("toHeight", toHeight.getRaw())
				.setParameter("limit", toHeight.getRaw() - fromHeight.getRaw());
		final List<Object[]> objects = listAndCast(query);
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

		return dbBlock;
	}

	private List<DbTransferTransaction> getDbTransfers(final long minBlockId, final long maxBlockId) {
		final Query query = this.getCurrentSession()
				.createSQLQuery("SELECT t.* FROM transfers t WHERE blockid > :minBlockId AND blockid < :maxBlockId AND senderproof IS NOT NULL ORDER BY blockid ASC")
				.setParameter("minBlockId", minBlockId)
				.setParameter("maxBlockId", maxBlockId);
		final List<Object[]> objects = listAndCast(query);
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

	private List<DbImportanceTransferTransaction> getDbImportanceTransfers(final long minBlockId, final long maxBlockId) {
		final Query query = this.getCurrentSession()
				.createSQLQuery("SELECT t.* FROM importancetransfers t WHERE blockid > :minBlockId AND blockid < :maxBlockId AND senderproof IS NOT NULL ORDER BY blockid ASC")
				.setParameter("minBlockId", minBlockId)
				.setParameter("maxBlockId", maxBlockId);
		final List<Object[]> objects = listAndCast(query);
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

	private List<DbMultisigAggregateModificationTransaction> getDbModificationTransactions(final long minBlockId, final long maxBlockId) {
		final String queryString = "SELECT msm.*, mm.multisigsignermodificationid as mmMultisigsignermodificationid, mm.id as mmId, " +
				"mm.cosignatoryid as mmCosignatoryid, mm.modificationtype as mmModificationtype FROM multisigsignermodifications msm " +
				"LEFT OUTER JOIN multisigmodifications mm ON mm.multisigsignermodificationid = msm.id " +
				"WHERE msm.blockid > :minBlockId AND msm.blockid < :maxBlockId AND msm.senderproof IS NOT NULL " +
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
		long curBlockId = 0L;
		for (Object[] array : arrays) {
			final long txid = castBigIntegerToLong((BigInteger)array[12]);
			if (curBlockId != txid) {
				curBlockId = txid;
				dbModificationTransaction = this.mapToDbModificationTransaction(array);
				dbModificationTransaction.setMultisigModifications(new HashSet<>());
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

	private DbAccount createDbAccount(final Long id) {
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
}
