package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.type.LongType;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.*;

@Repository
public class TransferDaoImpl implements TransferDao {
	private final SimpleTransferDaoImpl<DbTransferTransaction> impl;

	@Autowired(required = true)
	public TransferDaoImpl(final SessionFactory sessionFactory) {
		this.impl = new SimpleTransferDaoImpl<>("DbTransferTransaction", sessionFactory);
	}

	private Session getCurrentSession() {
		return this.impl.getCurrentSession();
	}

	//region SimpleTransferDao

	@Override
	@Transactional(readOnly = true)
	public Long count() {
		return this.impl.count();
	}

	@Override
	@Transactional(readOnly = true)
	public DbTransferTransaction findByHash(final byte[] txHash) {
		return this.impl.findByHash(txHash);
	}

	@Override
	@Transactional(readOnly = true)
	public DbTransferTransaction findByHash(final byte[] txHash, final long maxBlockHeight) {
		return this.impl.findByHash(txHash, maxBlockHeight);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean anyHashExists(final Collection<Hash> hashes, final BlockHeight maxBlockHeight) {
		return this.impl.anyHashExists(hashes, maxBlockHeight);
	}

	@Override
	@Transactional
	public void save(final DbTransferTransaction entity) {
		this.impl.save(entity);
	}

	@Override
	@Transactional
	public void saveMulti(final List<DbTransferTransaction> dbTransferTransactions) {
		this.impl.saveMulti(dbTransferTransactions);
	}

	//endregion

	// NOTE: this query will also ask for accounts of senders and recipients!
	@Override
	@Transactional(readOnly = true)
	public Collection<TransferBlockPair> getTransactionsForAccount(final Account address, final Integer timeStamp, final int limit) {
		// TODO: have no idea how to do it using Criteria...
		final Query query = this.getCurrentSession()
				.createQuery("select t, t.block from DbTransferTransaction t " +
						"where t.timeStamp <= :timeStamp AND (t.recipient.printableKey = :pubkey OR t.sender.printableKey = :pubkey) " +
						"order by t.timeStamp desc")
				.setParameter("timeStamp", timeStamp)
				.setParameter("pubkey", address.getAddress().getEncoded())
				.setMaxResults(limit);
		return executeQuery(query);
	}

	private String buildAddressQuery(final TransferType transferType) {
		switch (transferType) {
			case INCOMING:
				return "(t.recipient.id = :accountId)";
			case OUTGOING:
				return "(t.sender.id = :accountId)";
		}
		return "(t.recipient.id = :accountId OR t.sender.id = :accountId)";
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<TransferBlockPair> getTransactionsForAccountUsingHash(
			final Account address,
			final Hash hash,
			final BlockHeight height,
			final TransferType transferType,
			final int limit) {
		final Long accountId = this.getAccountId(address);
		if (null == accountId) {
			return new ArrayList<>();
		}

		long maxId = Long.MAX_VALUE;
		if (null != hash) {
			final String addressString = this.buildAddressQuery(transferType);
			maxId = this.getTransactionDescriptorUsingHash(accountId, hash, height, addressString).getTransfer().getId();
		}

		return this.getTransactionsForAccountUpToTransaction(accountId, maxId, limit, transferType);
	}

	private TransferBlockPair getTransactionDescriptorUsingHash(
			final Long accountId,
			final Hash hash,
			final BlockHeight height,
			final String addressString) {
		final Query prequery = this.getCurrentSession()
				.createQuery("select t, t.block from DbTransferTransaction t " +
						"WHERE " +
						addressString +
						" AND t.block.height = :height" +
						" ORDER BY t.timeStamp desc")
				.setParameter("height", height.getRaw())
				.setParameter("accountId", accountId);
		final List<TransferBlockPair> tempList = executeQuery(prequery);
		if (tempList.size() < 1) {
			throw new MissingResourceException("transaction not found in the db", Hash.class.toString(), hash.toString());
		}

		for (final TransferBlockPair pair : tempList) {
			if (pair.getTransfer().getTransferHash().equals(hash)) {
				return pair;
			}
		}

		throw new MissingResourceException("transaction not found in the db", Hash.class.toString(), hash.toString());
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<TransferBlockPair> getTransactionsForAccountUsingId(
			final Account address,
			final Long id,
			final TransferType transferType,
			final int limit) {
		final Long accountId = this.getAccountId(address);
		if (null == accountId) {
			return new ArrayList<>();
		}

		return null == id
				? this.getTransactionsForAccountUpToTransaction(accountId, Long.MAX_VALUE, limit, transferType)
				: this.getTransactionsForAccountUpToTransaction(accountId, id, limit, transferType);
	}

	private Long getAccountId(final Account account) {
		final Address address = account.getAddress();
		final Query query = this.getCurrentSession()
				.createSQLQuery("select id as accountId from accounts WHERE printablekey=:address")
				.addScalar("accountId", LongType.INSTANCE)
				.setParameter("address", address.getEncoded());
		return (Long)query.uniqueResult();
	}

	private Collection<TransferBlockPair> getTransactionsForAccountUpToTransaction(
			final Long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		if (TransferType.ALL == transferType) {
			final Collection<TransferBlockPair> pairs =
					this.getTransactionsForAccountUpToTransactionWithTransferType(accountId, maxId, limit, TransferType.INCOMING);
			pairs.addAll(this.getTransactionsForAccountUpToTransactionWithTransferType(accountId, maxId, limit, TransferType.OUTGOING));
			return this.sortAndLimit(pairs, limit);
		} else {
			final Collection<TransferBlockPair> pairs = this.getTransactionsForAccountUpToTransactionWithTransferType(
					accountId,
					maxId,
					limit,
					transferType);
			return this.sortAndLimit(pairs, limit);
		}
	}

	private List<TransferBlockPair> getTransactionsForAccountUpToTransactionWithTransferType(
			final Long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		final List<TransferBlockPair> pairs = new ArrayList<>();
		pairs.addAll(this.getTransfersForAccount(
				accountId,
				maxId,
				limit,
				transferType));
		/*pairs.addAll(this.getImportanceTransfersForAccount(
				accountId,
				maxId,
				limit,
				transferType));*/
		pairs.addAll(this.getMultisigTransfersForAccount(
				accountId,
				maxId,
				limit,
				transferType));
		pairs.addAll(this.getMultisigImportanceTransfersForAccount(
				accountId,
				maxId,
				limit,
				transferType));
		pairs.addAll(this.getMultisigMultisigSignerModificationsForAccount(
				accountId,
				maxId,
				limit,
				transferType));

		return pairs;
	}

	private List<TransferBlockPair> getTransfersForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		// TODO 20150111 J-G: should probably add test with senderProof NULL to test that it's being filtered (here and one other place too)
		//final String senderOrRecipient = TransferType.OUTGOING.equals(transferType) ? "t.senderId" : "t.recipientId";
		final String senderOrRecipient = TransferType.OUTGOING.equals(transferType) ? "sender.id" : "recipient.id";
		final Criteria criteria = this.getCurrentSession().createCriteria(DbTransferTransaction.class)
				.setFetchMode("blockId", FetchMode.JOIN)
				.setFetchMode("sender", FetchMode.JOIN)
				.setFetchMode("recipient", FetchMode.JOIN)
				.add(Restrictions.eq(senderOrRecipient, accountId))
				.add(Restrictions.isNotNull("senderProof"))
				.add(Restrictions.lt("id", maxId))
				.addOrder(Order.desc("id"))
				.setMaxResults(limit);
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		final List<DbTransferTransaction> list = criteria.list();
		return list.stream()
				.map(t -> {
					// force lazy initialization
					t.getBlock().getId();
					return new TransferBlockPair(t, t.getBlock());
				})
				.collect(Collectors.toList());
		/*final String queryString =
				"SELECT t.*, b.* FROM transfers t " +
						"LEFT OUTER JOIN Blocks b ON t.blockId = b.id " +
						"WHERE %s = %d AND t.senderProof IS NOT NULL AND t.id < %d " +
						"ORDER BY %s, t.id DESC";
		final String transfersQueryString = String.format(queryString,
				senderOrRecipient,
				accountId,
				maxId,
				senderOrRecipient);
		final Query query = this.getCurrentSession()
				.createSQLQuery(transfersQueryString)
				.addEntity(DbTransferTransaction.class)
				.addEntity(DbBlock.class)
				.setMaxResults(limit);
		return executeQuery(query);*/
	}

	private List<TransferBlockPair> getImportanceTransfersForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		// TODO 20150111 J-G: should probably add test with senderProof NULL to test that it's being filtered (here and one other place too)
		final String senderOrRecipient = TransferType.OUTGOING.equals(transferType) ? "t.senderId" : "t.remoteId";
		final String queryString =
				"SELECT t.*, b.* FROM ImportanceTransfers t " +
						"LEFT OUTER JOIN Blocks b ON t.blockId = b.id " +
						"WHERE %s = %d AND t.senderProof IS NOT NULL AND t.id < %d " +
						"ORDER BY %s, t.id DESC";
		final String transfersQueryString = String.format(queryString,
				senderOrRecipient,
				accountId,
				maxId,
				senderOrRecipient);
		final Query query = this.getCurrentSession()
				.createSQLQuery(transfersQueryString)
				.addEntity(DbImportanceTransferTransaction.class)
				.addEntity(DbBlock.class)
				.setMaxResults(limit);
		return executeQuery(query);
	}

	private List<TransferBlockPair> getMultisigTransfersForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		final List<TransactionIdBlockHeightPair> listOfIds = getMultisigIds(
				transferType,
				accountId,
				TransactionTypes.TRANSFER,
				maxId,
				limit);
		if (listOfIds.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigTransaction> transactions = this.getMultisigTransactions(listOfIds, "transferTransaction");
		final HashMap<Long, DbBlock> blockMap = this.getBlockMap(listOfIds);
		return IntStream.range(0, transactions.size())
				.mapToObj(i -> new TransferBlockPair(transactions.get(i), blockMap.get(listOfIds.get(i).blockHeight)))
				.collect(Collectors.toList());
	}

	private List<TransferBlockPair> getMultisigImportanceTransfersForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		final List<TransactionIdBlockHeightPair> listOfIds = getMultisigIds(
				transferType,
				accountId,
				TransactionTypes.IMPORTANCE_TRANSFER,
				maxId,
				limit);
		if (listOfIds.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigTransaction> transactions = this.getMultisigTransactions(listOfIds, "importanceTransferTransaction");
		final HashMap<Long, DbBlock> blockMap = this.getBlockMap(listOfIds);
		return IntStream.range(0, transactions.size())
				.mapToObj(i -> new TransferBlockPair(transactions.get(i), blockMap.get(listOfIds.get(i).blockHeight)))
				.collect(Collectors.toList());
	}

	private List<TransferBlockPair> getMultisigMultisigSignerModificationsForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		final List<TransactionIdBlockHeightPair> listOfIds = getMultisigIds(
				transferType,
				accountId,
				TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
				maxId,
				limit);
		if (listOfIds.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigTransaction> transactions = this.getMultisigTransactions(listOfIds, "multisigAggregateModificationTransaction");
		final HashMap<Long, DbBlock> blockMap = this.getBlockMap(listOfIds);
		return IntStream.range(0, transactions.size())
				.mapToObj(i -> new TransferBlockPair(transactions.get(i), blockMap.get(listOfIds.get(i).blockHeight)))
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private List<TransactionIdBlockHeightPair> getMultisigIds(
			final TransferType transferType,
			final long accountId,
			final int type,
			final long maxId,
			final int limit) {
		final String table = TransferType.OUTGOING.equals(transferType) ? "multisigsends" : "multisigreceives";
		final String preQueryTemplate = "SELECT transactionId, height FROM " + table + " WHERE accountId=%d AND type=%d AND transactionId < %d ORDER BY transactionId DESC";
		final String preQueryString = String.format(preQueryTemplate, accountId, type, maxId);
		final Query preQuery = this.getCurrentSession()
				.createSQLQuery(preQueryString)
				.addScalar("transactionId", LongType.INSTANCE)
				.addScalar("height", LongType.INSTANCE)
				.setMaxResults(limit);
		final List<Object[]> list = preQuery.list();
		return list.stream().map(o -> new TransactionIdBlockHeightPair((Long)o[0], (Long)o[1])).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private static List<TransferBlockPair> executeQuery(final Query q) {
		final List<Object[]> list = q.list();
		return list.stream().map(o -> new TransferBlockPair((AbstractBlockTransfer)o[0], (DbBlock)o[1])).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private List<DbMultisigTransaction> getMultisigTransactions(final List<TransactionIdBlockHeightPair> pairs, final String joinEntity) {
		final Criteria criteria = this.getCurrentSession().createCriteria(DbMultisigTransaction.class)
				.setFetchMode(joinEntity, FetchMode.JOIN)
				.add(Restrictions.in("id", pairs.stream().map(p -> p.transactionId).collect(Collectors.toList())))
				.add(Restrictions.isNotNull(joinEntity))
				.addOrder(Order.desc("id"));
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return criteria.list();
	}

	@SuppressWarnings("unchecked")
	private HashMap<Long, DbBlock> getBlockMap(final List<TransactionIdBlockHeightPair> pairs) {
		final HashMap<Long, DbBlock> blockMap = new HashMap<>();
		final Criteria criteria = this.getCurrentSession().createCriteria(DbBlock.class)
				.add(Restrictions.in("height", pairs.stream().map(p -> p.blockHeight).collect(Collectors.toList())))
				.addOrder(Order.desc("height"));
		final List<DbBlock> blocks = criteria.list();
		blocks.stream().forEach(b -> blockMap.put(b.getHeight(), b));
		return blockMap;
	}

	private Collection<TransferBlockPair> sortAndLimit(final Collection<TransferBlockPair> pairs, final int limit) {
		final List<TransferBlockPair> list = pairs.stream()
				.sorted(this::comparePair)
				.collect(Collectors.toList());
		TransferBlockPair curPair = null;
		final Collection<TransferBlockPair> result = new ArrayList<>();
		for (final TransferBlockPair pair : list) {
			if (null == curPair || !(curPair.getTransfer().getId().equals(pair.getTransfer().getId()))) {
				result.add(pair);
				if (limit == result.size()) {
					break;
				}
			}
			curPair = pair;
		}

		return result;
	}

	private int comparePair(final TransferBlockPair lhs, final TransferBlockPair rhs) {
		// TODO 2014 J-B: check with G about if we still need to compare getBlkIndex
		return -lhs.getTransfer().getId().compareTo(rhs.getTransfer().getId());
	}

	private class TransactionIdBlockHeightPair {
		private final Long transactionId;
		private final Long blockHeight;

		private TransactionIdBlockHeightPair(final Long transactionId, final Long blockHeight) {
			this.transactionId = transactionId;
			this.blockHeight = blockHeight;
		}
	}
}
