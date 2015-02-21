package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.type.LongType;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dao.retrievers.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.TransactionRegistry;
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

		return this.getTransactionsForAccountUsingId(address, maxId, transferType, limit);
	}

	// TODO 20150126 BR -> BR: have to think how to do this in a smart way. We are not using it right now.
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

		final long maxId = null == id ? Long.MAX_VALUE : id;
		return this.getTransactionsForAccountUpToTransaction(accountId, maxId, limit, transferType);
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
			// note that we have to do separate queries for incoming and outgoing transactions since otherwise h2
			// is not able to use an index to speed up the query.
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

	private Collection<TransferBlockPair> getTransactionsForAccountUpToTransactionWithTransferType(
			final Long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		final Collection<TransferBlockPair> pairs = new ArrayList<>();
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			pairs.addAll(entry.getFromDb.apply(this, accountId, maxId, limit, transferType));
		}

		return pairs;
	}

	@Override
	public Collection<TransferBlockPair> getTransfersForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		// TODO 20150212 J-J: this will come from the registry
		return new TransferRetriever().getTransfersForAccount(
				this.getCurrentSession(),
				accountId,
				maxId,
				limit,
				transferType);
	}

	@Override
	public Collection<TransferBlockPair> getImportanceTransfersForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		return new ImportanceTransferRetriever().getTransfersForAccount(
				this.getCurrentSession(),
				accountId,
				maxId,
				limit,
				transferType);
	}

	@Override
	public Collection<TransferBlockPair> getMultisigSignerModificationsForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		return new MultisigModificationRetriever().getTransfersForAccount(
				this.getCurrentSession(),
				accountId,
				maxId,
				limit,
				transferType);
	}

	@Override
	public Collection<TransferBlockPair> getMultisigTransactionsForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		// TODO 20150127 J-G: should we also have a registry of sorts for this?
		final Collection<TransferBlockPair> pairs = this.getMultisigTransfersForAccount(accountId, maxId, limit, transferType);
		pairs.addAll(this.getMultisigImportanceTransfersForAccount(accountId, maxId, limit, transferType));
		pairs.addAll(this.getMultisigMultisigSignerModificationsForAccount(accountId, maxId, limit, transferType));
		return pairs;
	}

	private Collection<TransferBlockPair> getMultisigTransfersForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		final List<TransactionIdBlockHeightPair> listOfIds = this.getMultisigIds(
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

	private Collection<TransferBlockPair> getMultisigImportanceTransfersForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		final List<TransactionIdBlockHeightPair> listOfIds = this.getMultisigIds(
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

	private Collection<TransferBlockPair> getMultisigMultisigSignerModificationsForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		final List<TransactionIdBlockHeightPair> listOfIds = this.getMultisigIds(
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

	private List<TransactionIdBlockHeightPair> getMultisigIds(
			final TransferType transferType,
			final long accountId,
			final int type,
			final long maxId,
			final int limit) {
		final String table = TransferType.OUTGOING.equals(transferType) ? "multisigsends" : "multisigreceives";
		final String preQueryTemplate =
				"SELECT transactionId, height FROM " + table +
						" WHERE accountId=%d AND type=%d AND transactionId < %d " +
						"ORDER BY accountId asc, type asc, transactionId DESC";
		final String preQueryString = String.format(preQueryTemplate, accountId, type, maxId);
		final Query preQuery = this.getCurrentSession()
				.createSQLQuery(preQueryString)
				.addScalar("transactionId", LongType.INSTANCE)
				.addScalar("height", LongType.INSTANCE)
				.setMaxResults(limit);
		final List<Object[]> list = listAndCast(preQuery);
		return list.stream().map(o -> new TransactionIdBlockHeightPair((Long)o[0], (Long)o[1])).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private static List<TransferBlockPair> executeQuery(final Query q) {
		final List<Object[]> list = q.list();
		return list.stream().map(o -> new TransferBlockPair((AbstractBlockTransfer)o[0], (DbBlock)o[1])).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> listAndCast(final Query query) {
		return (List<T>)query.list();
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> listAndCast(final Criteria criteria) {
		return (List<T>)criteria.list();
	}

	private List<DbMultisigTransaction> getMultisigTransactions(final List<TransactionIdBlockHeightPair> pairs, final String joinEntity) {
		final Criteria criteria = this.getCurrentSession().createCriteria(DbMultisigTransaction.class)
				.setFetchMode(joinEntity, FetchMode.JOIN)
				.add(Restrictions.in("id", pairs.stream().map(p -> p.transactionId).collect(Collectors.toList())))
				.add(Restrictions.isNotNull(joinEntity))
				.addOrder(Order.desc("id"));
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return listAndCast(criteria);
	}

	private HashMap<Long, DbBlock> getBlockMap(final List<TransactionIdBlockHeightPair> pairs) {
		final HashMap<Long, DbBlock> blockMap = new HashMap<>();
		final Criteria criteria = this.getCurrentSession().createCriteria(DbBlock.class)
				.add(Restrictions.in("height", pairs.stream().map(p -> p.blockHeight).collect(Collectors.toList())))
				.addOrder(Order.desc("height"));
		final List<DbBlock> blocks = listAndCast(criteria);
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
