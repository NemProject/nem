package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.type.LongType;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ByteUtils;
import org.nem.nis.BlockLoader;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.TransactionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Repository
public class BlockDaoImpl implements BlockDao {
	private static final Logger LOGGER = Logger.getLogger(BlockDaoImpl.class.getName());

	private final SessionFactory sessionFactory;

	@Autowired(required = true)
	public BlockDaoImpl(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	private <TDbModel extends AbstractBlockTransfer> void saveSingleBlock(final DbBlock block) {
		this.getCurrentSession().saveOrUpdate(block);
		final ArrayList<DbMultisigSend> sendList = new ArrayList<>(100);
		final ArrayList<DbMultisigReceive> receiveList = new ArrayList<>(100);

		// TODO 20150122 BR -> G, J: should the DbBlock create empty lists for the different transaction types or is that a problem for hibernate?
		if (null == block.getBlockMultisigTransactions()) {
			return;
		}

		final TransactionRegistry.Entry<DbMultisigTransaction, ?> multisigEntry
				= (TransactionRegistry.Entry<DbMultisigTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.MULTISIG);
		final List<DbMultisigTransaction> multisigTransactions = multisigEntry.getFromBlock.apply(block);
		for (final DbMultisigTransaction transaction : multisigTransactions) {
			final Long height = block.getHeight();
			final Long id = transaction.getId();
			int txType = 0;
			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				final TransactionRegistry.Entry<TDbModel, ?> theEntry = (TransactionRegistry.Entry<TDbModel, ?>)entry;
				final TDbModel transfer = theEntry.getFromMultisig.apply(transaction);
				if (null == transfer) {
					continue;
				}

				txType = theEntry.type;
				sendList.add(this.createSend(transfer.getSender().getId(), theEntry.type, height, id));

				final DbAccount recipient = theEntry.getRecipient.apply(transfer);
				if (null != recipient) {
					receiveList.add(this.createReceive(recipient.getId(), theEntry.type, height, id));
				}

				theEntry.getOtherAccounts.apply(transfer).stream()
						.forEach(a -> receiveList.add(this.createReceive(a.getId(), theEntry.type, height, id)));
			}

			sendList.add(0, this.createSend(transaction.getSender().getId(), txType, height, id));
			for (final DbAccount account : multisigEntry.getOtherAccounts.apply(transaction)) {
				sendList.add(this.createSend(account.getId(), txType, height, id));
			}
		}

		for (final DbMultisigSend send : sendList) {
			this.getCurrentSession().saveOrUpdate(send);
		}

		for (final DbMultisigReceive receive : receiveList) {
			this.getCurrentSession().saveOrUpdate(receive);
		}
	}

	private DbMultisigSend createSend(
			final Long accountId,
			final Integer type,
			final Long height,
			final Long transactionId) {
		final DbMultisigSend send = new DbMultisigSend();
		send.setAccountId(accountId);
		send.setType(type);
		send.setHeight(height);
		send.setTransactionId(transactionId);
		return send;
	}

	private DbMultisigReceive createReceive(
			final Long accountId,
			final Integer type,
			final Long height,
			final Long transactionId) {
		final DbMultisigReceive receive = new DbMultisigReceive();
		receive.setAccountId(accountId);
		receive.setType(type);
		receive.setHeight(height);
		receive.setTransactionId(transactionId);
		return receive;
	}

	@Override
	@Transactional
	public void save(final DbBlock block) {
		this.saveSingleBlock(block);

		this.getCurrentSession().flush();
		this.getCurrentSession().clear();
	}

	// TODO 20141206 J-G: does it make sense to add a test for this?
	@Override
	@Transactional
	public void save(final List<DbBlock> dbBlocks) {
		for (final DbBlock block : dbBlocks) {
			this.saveSingleBlock(block);
		}
		this.getCurrentSession().flush();
		this.getCurrentSession().clear();
	}

	@Override
	@Transactional(readOnly = true)
	public Long count() {
		return (Long)this.getCurrentSession().createQuery("select count (*) from DbBlock").uniqueResult();
	}

	// NOTE: remember to modify deleteBlocksAfterHeight TOO!
	private static Criteria setTransfersFetchMode(final Criteria criteria, final FetchMode fetchMode) {
		return criteria
				.setFetchMode("blockTransferTransactions", fetchMode)
				.setFetchMode("blockImportanceTransferTransactions", fetchMode)
				.setFetchMode("blockMultisigAggregateModificationTransactions", fetchMode)
				.setFetchMode("blockMultisigTransactions", fetchMode);
	}

	private static Criteria setTransfersToJoin(final Criteria criteria) {
		return setTransfersFetchMode(criteria, FetchMode.JOIN);
	}

	private static Criteria setTransfersToSelect(final Criteria criteria) {
		return setTransfersFetchMode(criteria, FetchMode.SELECT);
	}

	//region find*
	@Override
	@Transactional(readOnly = true)
	public DbBlock findByHeight(final BlockHeight height) {
		final Criteria criteria = setTransfersToJoin(this.getCurrentSession().createCriteria(DbBlock.class))
				.add(Restrictions.eq("height", height.getRaw()));
		return this.executeSingleQuery(criteria);
	}

	/**
	 * First try to find block using "shortId",
	 * than find proper block in software.
	 */
	@Override
	@Transactional(readOnly = true)
	public DbBlock findByHash(final Hash blockHash) {
		final byte[] blockHashBytes = blockHash.getRaw();
		final long blockId = ByteUtils.bytesToLong(blockHashBytes);

		final Criteria criteria = setTransfersToJoin(this.getCurrentSession().createCriteria(DbBlock.class))
				.add(Restrictions.eq("shortId", blockId));
		final List<DbBlock> blockList = listAndCast(criteria);

		for (final Object blockObject : blockList) {
			final DbBlock block = (DbBlock)blockObject;
			if (Arrays.equals(blockHashBytes, block.getBlockHash().getRaw())) {
				return block;
			}
		}
		return null;
	}
	//endregion

	@Override
	@Transactional(readOnly = true)
	public HashChain getHashesFrom(final BlockHeight height, final int limit) {
		final List<byte[]> blockList = this.prepareCriteriaGetFor("blockHash", height, limit);
		return HashChain.fromRawHashes(blockList);
	}

	@Override
	@Transactional(readOnly = true)
	public List<BlockDifficulty> getDifficultiesFrom(final BlockHeight height, final int limit) {
		final List<Long> rawDifficulties = this.prepareCriteriaGetFor("difficulty", height, limit);
		final List<BlockDifficulty> result = new ArrayList<>(rawDifficulties.size());
		for (final Long elem : rawDifficulties) {
			result.add(new BlockDifficulty(elem));
		}
		return result;
		//return rawDifficulties.stream().map(diff -> new BlockDifficulty(diff)).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<TimeInstant> getTimeStampsFrom(final BlockHeight height, final int limit) {
		final List<Integer> rawTimeStamps = this.prepareCriteriaGetFor("timeStamp", height, limit);
		return rawTimeStamps.stream().map(TimeInstant::new).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<DbBlock> getBlocksForAccount(final Account account, final Hash hash, final int limit) {
		final long height = null == hash ? Long.MAX_VALUE : this.findByHash(hash).getHeight();
		return this.getLatestBlocksForAccount(account, height, limit);
	}

	private Collection<DbBlock> getLatestBlocksForAccount(final Account account, final long height, final int limit) {
		// note that since h2 is not very good in optimizing the execution plan we should limit the returned
		// result set as early as possible or it will scan through the entire index.
		// note also that it is better to use a union because otherwise we have an OR in the where clause which
		// will prevent h2 from using an index to speed up the query.
		final Long accountId = this.getAccountId(account);
		final String queryString = "((SELECT b.* FROM blocks b WHERE height<:height AND harvesterId=:accountId limit :limit) UNION " +
				"(SELECT b.* FROM blocks b WHERE height<:height AND harvestedInName=:accountId limit :limit)) " +
				"ORDER BY height DESC limit :limit";
		final Query query = this.getCurrentSession()
				.createSQLQuery(queryString)
				.addEntity(DbBlock.class)
				.setParameter("height", height)
				.setParameter("accountId", accountId)
				.setParameter("limit", limit);
		return listAndCast(query);
	}

	private Long getAccountId(final Account account) {
		final Address address = account.getAddress();
		final Query query = this.getCurrentSession()
				.createSQLQuery("SELECT id AS accountId FROM accounts WHERE printableKey=:address")
				.addScalar("accountId", LongType.INSTANCE)
				.setParameter("address", address.getEncoded());
		return (Long)query.uniqueResult();
	}

	@Override
	@Transactional
	public Collection<DbBlock> getBlocksAfter(final BlockHeight height, final int limit) {
		final BlockLoader blockLoader = new BlockLoader(this.sessionFactory);
		final long start = System.currentTimeMillis();
		final List<DbBlock> dbBlocks = blockLoader.loadBlocks(height, new BlockHeight(height.getRaw() + limit));
		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("loadBlocks (from height %d to height %d) needed %dms", height.getRaw() + 1, height.getRaw() + limit, stop - start));
		return dbBlocks;
	}

	@Override
	@Transactional
	public void deleteBlocksAfterHeight(final BlockHeight blockHeight) {
		// apparently delete on blocks is not enough, as
		// delete is not cascaded :/
		//
		// "A delete operation only applies to entities of the specified class and its subclasses.
		//  It does not cascade to related entities."


		// DbMultisigTransaction needs to dropped first because it has foreign key references to the other
		// transaction tables; attempting to delete other transactions first will break referential integrity
		this.dropTransfers(
				blockHeight,
				"DbMultisigTransaction",
				"blockMultisigTransactions",
				transactionsToDelete -> {
					final Query preQuery = this.getCurrentSession()
							.createQuery("delete from DbMultisigSignatureTransaction m where m.multisigTransaction.id in (:ids)")
							.setParameterList("ids", transactionsToDelete);
					preQuery.executeUpdate();
				});

		this.dropTransfers(blockHeight, "DbTransferTransaction", "blockTransferTransactions", v -> {});
		this.dropTransfers(blockHeight, "DbImportanceTransferTransaction", "blockImportanceTransferTransactions", v -> {});
		this.dropTransfers(
				blockHeight,
				"DbMultisigAggregateModificationTransaction",
				"blockMultisigAggregateModificationTransactions",
				transactionsToDelete -> {
					final Query preQuery = this.getCurrentSession()
							.createQuery("delete from DbMultisigModification m where m.multisigAggregateModificationTransaction.id in (:ids)")
							.setParameterList("ids", transactionsToDelete);
					preQuery.executeUpdate();
				});

		final Query query = this.getCurrentSession()
				.createQuery("delete from DbBlock a where a.height > :height")
				.setParameter("height", blockHeight.getRaw());
		query.executeUpdate();
	}

	private void dropTransfers(final BlockHeight blockHeight, final String tableName, final String transfersName, final Consumer<List<Long>> preQuery) {
		final Query getTransactionIdsQuery = this.getCurrentSession()
				.createQuery("select tx.id from DbBlock b join b." + transfersName + " tx where b.height > :height")
				.setParameter("height", blockHeight.getRaw());
		final List<Long> transactionsToDelete = listAndCast(getTransactionIdsQuery);

		if (!transactionsToDelete.isEmpty()) {
			preQuery.accept(transactionsToDelete);

			final Query dropTxes = this.getCurrentSession()
					.createQuery("delete from " + tableName + " t where t.id in (:ids)")
					.setParameterList("ids", transactionsToDelete);
			dropTxes.executeUpdate();
		}
	}

	private <T> T executeSingleQuery(final Criteria criteria) {
		final List<T> blockList = listAndCast(criteria);
		return !blockList.isEmpty() ? blockList.get(0) : null;
	}

	private <T> List<T> prepareCriteriaGetFor(final String name, final BlockHeight height, final int limit) {
		final Criteria criteria = this.getCurrentSession().createCriteria(DbBlock.class)
				.setMaxResults(limit)
				.add(Restrictions.ge("height", height.getRaw())) // >=
				.setProjection(Projections.property(name))
				.addOrder(Order.asc("height"));
		return listAndCast(criteria);
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> listAndCast(final Query q) {
		return q.list();
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> listAndCast(final Criteria criteria) {
		return criteria.list();
	}
}
