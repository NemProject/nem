package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.type.LongType;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ByteUtils;
import org.nem.nis.dbmodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Repository
public class BlockDaoImpl implements BlockDao {

	private final SessionFactory sessionFactory;

	@Autowired(required = true)
	public BlockDaoImpl(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	private void saveSingleBlock(final DbBlock block) {
		this.getCurrentSession().saveOrUpdate(block);
		ArrayList<DbMultisigSend> sendList = new ArrayList<>(100);
		ArrayList<DbMultisigReceive> receiveList = new ArrayList<>(100);

		// TODO 20150127 J-B: can we delete this commented out code?
		// TODO 20150127 J-B: can you possibly refactor some of this to use the TransactionRegistry?
		// > (or your own dao registry?)
// We'll be able to use it for other queries too... just need to filter out txes with null sig
//
//		for (final DbTransferTransaction transaction : block.getBlockTransferTransactions()) {
//			final DbMultisigSend t = new DbMultisigSend();
//			t.setAccountId(transaction.getSender().getId());
//			t.setHeight(block.getHeight());
//			t.setTransactionId(transaction.getId());
//			list.add(t);
//		}
//
//		for (final DbImportanceTransferTransaction transaction : block.getBlockImportanceTransferTransactions()) {
//			final DbMultisigSend t = new DbMultisigSend();
//			t.setAccountId(transaction.getSender().getId());
//			t.setHeight(block.getHeight());
//			t.setTransactionId(transaction.getId());
//			list.add(t);
//		}
//
//		for (final DbMultisigAggregateModificationTransaction transaction : block.getBlockMultisigAggregateModificationTransactions()) {
//			final DbMultisigSend t = new DbMultisigSend();
//			t.setAccountId(transaction.getSender().getId());
//			t.setHeight(block.getHeight());
//			t.setTransactionId(transaction.getId());
//			list.add(t);
//
//			for (final DbMultisigModification modification : transaction.getMultisigModifications()) {
//				final DbMultisigSend sub = new DbMultisigSend();
//				sub.setAccountId(modification.getCosignatory().getId());
//				sub.setHeight(block.getHeight());
//				// we're using transaction id....
//				sub.setTransactionId(transaction.getId());
//				list.add(t);
//			}
//		}

		// TODO 20150122 BR -> G, J: should the DbBlock create empty lists for the different transaction types or is that a problem for hibernate?
		if (null == block.getBlockMultisigTransactions()) {
			return;
		}

		int txType = 0;
		for (final DbMultisigTransaction transaction : block.getBlockMultisigTransactions()) {
			if (transaction.getTransferTransaction() != null) {
				txType = TransactionTypes.TRANSFER;
				final DbMultisigSend innerSend = new DbMultisigSend();
				innerSend.setAccountId(transaction.getTransferTransaction().getSender().getId());
				innerSend.setType(txType);
				innerSend.setHeight(block.getHeight());
				innerSend.setTransactionId(transaction.getId());
				sendList.add(innerSend);
				final DbMultisigReceive innerReceive = new DbMultisigReceive();
				innerReceive.setAccountId(transaction.getTransferTransaction().getRecipient().getId());
				innerReceive.setType(txType);
				innerReceive.setHeight(block.getHeight());
				innerReceive.setTransactionId(transaction.getId());
				receiveList.add(innerReceive);
			} else if (transaction.getImportanceTransferTransaction() != null) {
				txType = TransactionTypes.IMPORTANCE_TRANSFER;
				final DbMultisigSend inner = new DbMultisigSend();
				inner.setAccountId(transaction.getImportanceTransferTransaction().getSender().getId());
				inner.setType(txType);
				inner.setHeight(block.getHeight());
				inner.setTransactionId(transaction.getId());
				sendList.add(inner);
				final DbMultisigReceive innerReceive = new DbMultisigReceive();
				innerReceive.setAccountId(transaction.getImportanceTransferTransaction().getRemote().getId());
				innerReceive.setType(txType);
				innerReceive.setHeight(block.getHeight());
				innerReceive.setTransactionId(transaction.getId());
				receiveList.add(innerReceive);
			} else if (transaction.getMultisigAggregateModificationTransaction() != null) {
				final DbMultisigAggregateModificationTransaction aggregate = transaction.getMultisigAggregateModificationTransaction();
				final DbMultisigSend inner = new DbMultisigSend();
				inner.setAccountId(aggregate.getSender().getId());
				inner.setType(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION);
				inner.setHeight(block.getHeight());
				inner.setTransactionId(transaction.getId());
				sendList.add(inner);
				txType = TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION;

				for (final DbMultisigModification modification : aggregate.getMultisigModifications()) {
					final DbMultisigReceive modificationReceive = new DbMultisigReceive();
					modificationReceive.setAccountId(modification.getCosignatory().getId());
					modificationReceive.setType(txType);
					modificationReceive.setHeight(block.getHeight());
					modificationReceive.setTransactionId(transaction.getId());
					receiveList.add(modificationReceive);
				}
			}

			final DbMultisigSend t = new DbMultisigSend();
			t.setAccountId(transaction.getSender().getId());
			t.setType(txType);
			t.setHeight(block.getHeight());
			t.setTransactionId(transaction.getId());
			sendList.add(0, t);

			for (final DbMultisigSignatureTransaction signatureTransaction : transaction.getMultisigSignatureTransactions()) {
				final DbMultisigSend sub = new DbMultisigSend();
				sub.setAccountId(signatureTransaction.getSender().getId());
				sub.setType(txType);
				sub.setHeight(block.getHeight());
				sub.setTransactionId(transaction.getId());
				sendList.add(sub);
			}
		}

		for (final DbMultisigSend send : sendList) {
			this.getCurrentSession().saveOrUpdate(send);
		}

		for (final DbMultisigReceive receive : receiveList) {
			this.getCurrentSession().saveOrUpdate(receive);
		}
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
		final Long accountId = this.getAccountId(account);
		final String preQueryString = "(SELECT b.* FROM blocks b WHERE height<%d AND harvesterId=%d UNION " +
				"SELECT b.* FROM blocks b WHERE height<%d AND harvestedInName=%d) " +
				"ORDER BY height DESC";
		// TODO 20150108 J-B: any reason you're using String.format instead of setting parameters on the query?
		final String queryString = String.format(
				preQueryString,
				height,
				accountId,
				height,
				accountId);
		final Query query = this.getCurrentSession()
				.createSQLQuery(queryString)
				.addEntity(DbBlock.class)
				.setMaxResults(limit);
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
		// whatever it takes : DO NOT ADD setMaxResults here!
		final long blockHeight = height.getRaw();
		final Criteria criteria = setTransfersToJoin(this.getCurrentSession().createCriteria(DbBlock.class))
				.setFetchMode("harvester", FetchMode.JOIN)
				.add(Restrictions.gt("height", blockHeight))
				.add(Restrictions.le("height", blockHeight + limit))
				.addOrder(Order.asc("height"));
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return listAndCast(criteria);
	}

	@Override
	@Transactional
	public void deleteBlocksAfterHeight(final BlockHeight blockHeight) {
		// apparently delete on blocks is not enough, as
		// delete is not cascaded :/
		//
		// "A delete operation only applies to entities of the specified class and its subclasses.
		//  It does not cascade to related entities."

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

		// must be last
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
