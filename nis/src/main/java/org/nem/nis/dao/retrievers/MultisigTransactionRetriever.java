package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.type.LongType;
import org.nem.core.model.TransactionTypes;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.*;

/**
 * Class for for retrieving multisig transactions.
 */
public class MultisigTransactionRetriever implements TransactionRetriever {

	@Override
	public Collection<TransferBlockPair> getTransfersForAccount(
			final Session session,
			final long accountId,
			final long maxId,
			final int limit,
			final ReadOnlyTransferDao.TransferType transferType) {
		if ( ReadOnlyTransferDao.TransferType.ALL == transferType) {
			throw new RuntimeException("transfer type ALL not supported by transaction retriever classes");
		}

		// TODO 20150127 J-G: should we also have a registry of sorts for this?
		final Collection<TransferBlockPair> pairs = this.getMultisigTransfersForAccount(session, accountId, maxId, limit, transferType);
		pairs.addAll(this.getMultisigImportanceTransfersForAccount(session, accountId, maxId, limit, transferType));
		pairs.addAll(this.getMultisigMultisigSignerModificationsForAccount(session, accountId, maxId, limit, transferType));
		return this.sortAndLimit(pairs, limit);
	}

	private Collection<TransferBlockPair> getMultisigTransfersForAccount(
			final Session session,
			final long accountId,
			final long maxId,
			final int limit,
			final ReadOnlyTransferDao.TransferType transferType) {
		final List<TransactionIdBlockHeightPair> listOfIds = this.getMultisigIds(
				session,
				transferType,
				accountId,
				TransactionTypes.TRANSFER,
				maxId,
				limit);
		if (listOfIds.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigTransaction> transactions = this.getMultisigTransactions(session, listOfIds, "transferTransaction");
		final HashMap<Long, DbBlock> blockMap = this.getBlockMap(session, listOfIds);
		return IntStream.range(0, transactions.size())
				.mapToObj(i -> new TransferBlockPair(transactions.get(i), blockMap.get(listOfIds.get(i).blockHeight)))
				.collect(Collectors.toList());
	}

	private Collection<TransferBlockPair> getMultisigImportanceTransfersForAccount(
			final Session session,
			final long accountId,
			final long maxId,
			final int limit,
			final ReadOnlyTransferDao.TransferType transferType) {
		final List<TransactionIdBlockHeightPair> listOfIds = this.getMultisigIds(
				session,
				transferType,
				accountId,
				TransactionTypes.IMPORTANCE_TRANSFER,
				maxId,
				limit);
		if (listOfIds.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigTransaction> transactions = this.getMultisigTransactions(session, listOfIds, "importanceTransferTransaction");
		final HashMap<Long, DbBlock> blockMap = this.getBlockMap(session, listOfIds);
		return IntStream.range(0, transactions.size())
				.mapToObj(i -> new TransferBlockPair(transactions.get(i), blockMap.get(listOfIds.get(i).blockHeight)))
				.collect(Collectors.toList());
	}

	private Collection<TransferBlockPair> getMultisigMultisigSignerModificationsForAccount(
			final Session session,
			final long accountId,
			final long maxId,
			final int limit,
			final ReadOnlyTransferDao.TransferType transferType) {
		final List<TransactionIdBlockHeightPair> listOfIds = this.getMultisigIds(
				session,
				transferType,
				accountId,
				TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
				maxId,
				limit);
		if (listOfIds.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigTransaction> transactions = this.getMultisigTransactions(session, listOfIds, "multisigAggregateModificationTransaction");
		final HashMap<Long, DbBlock> blockMap = this.getBlockMap(session, listOfIds);
		return IntStream.range(0, transactions.size())
				.mapToObj(i -> new TransferBlockPair(transactions.get(i), blockMap.get(listOfIds.get(i).blockHeight)))
				.collect(Collectors.toList());
	}

	private List<TransactionIdBlockHeightPair> getMultisigIds(
			final Session session,
			final ReadOnlyTransferDao.TransferType transferType,
			final long accountId,
			final int type,
			final long maxId,
			final int limit) {
		final String table = ReadOnlyTransferDao.TransferType.OUTGOING.equals(transferType) ? "multisigsends" : "multisigreceives";
		final String preQueryTemplate =
				"SELECT transactionId, height FROM " + table +
						" WHERE accountId=%d AND type=%d AND transactionId < %d " +
						"ORDER BY accountId asc, type asc, transactionId DESC";
		final String preQueryString = String.format(preQueryTemplate, accountId, type, maxId);
		final Query preQuery = session
				.createSQLQuery(preQueryString)
				.addScalar("transactionId", LongType.INSTANCE)
				.addScalar("height", LongType.INSTANCE)
				.setMaxResults(limit);
		final List<Object[]> list = HibernateUtils.listAndCast(preQuery);
		return list.stream().map(o -> new TransactionIdBlockHeightPair((Long)o[0], (Long)o[1])).collect(Collectors.toList());
	}

	private List<DbMultisigTransaction> getMultisigTransactions(
			final Session session,
			final List<TransactionIdBlockHeightPair> pairs,
			final String joinEntity) {
		final Criteria criteria = session.createCriteria(DbMultisigTransaction.class)
				.setFetchMode(joinEntity, FetchMode.JOIN)
				.add(Restrictions.in("id", pairs.stream().map(p -> p.transactionId).collect(Collectors.toList())))
				.add(Restrictions.isNotNull(joinEntity))
				.addOrder(Order.desc("id"));
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return HibernateUtils.listAndCast(criteria);
	}

	private HashMap<Long, DbBlock> getBlockMap(final Session session, final List<TransactionIdBlockHeightPair> pairs) {
		final HashMap<Long, DbBlock> blockMap = new HashMap<>();
		final Criteria criteria = session.createCriteria(DbBlock.class)
				.add(Restrictions.in("height", pairs.stream().map(p -> p.blockHeight).collect(Collectors.toList())))
				.addOrder(Order.desc("height"));
		final List<DbBlock> blocks = HibernateUtils.listAndCast(criteria);
		blocks.stream().forEach(b -> blockMap.put(b.getHeight(), b));
		return blockMap;
	}

	private Collection<TransferBlockPair> sortAndLimit(final Collection<TransferBlockPair> pairs, final int limit) {
		final List<TransferBlockPair> list = pairs.stream()
				.sorted()
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

	private class TransactionIdBlockHeightPair {
		private final Long transactionId;
		private final Long blockHeight;

		private TransactionIdBlockHeightPair(final Long transactionId, final Long blockHeight) {
			this.transactionId = transactionId;
			this.blockHeight = blockHeight;
		}
	}
}
