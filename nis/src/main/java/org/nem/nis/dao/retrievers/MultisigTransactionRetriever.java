package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.type.LongType;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.TransactionRegistry;

import java.util.*;
import java.util.stream.*;

/**
 * Class for for retrieving multisig transactions.
 */
@SuppressWarnings("rawtypes")
public class MultisigTransactionRetriever implements TransactionRetriever {
	private static final Map<Integer, String> TYPE_TO_FIELD_NAME_MAP = TransactionRegistry.stream().filter(e -> null != e.multisigJoinField)
			.collect(Collectors.toMap(e -> e.type, e -> e.multisigJoinField));

	@Override
	public Collection<TransferBlockPair> getTransfersForAccount(final Session session, final long accountId, final long maxId,
			final int limit, final ReadOnlyTransferDao.TransferType transferType) {
		if (ReadOnlyTransferDao.TransferType.ALL == transferType) {
			throw new IllegalArgumentException("transfer type ALL not supported by transaction retriever classes");
		}

		final Collection<TransferBlockPair> pairs = new ArrayList<>();
		for (final Map.Entry<Integer, String> entry : TYPE_TO_FIELD_NAME_MAP.entrySet()) {
			pairs.addAll(this.getMultisigTransactionsForAccount(session, accountId, maxId, limit, transferType, entry.getKey(),
					entry.getValue()));
		}
		return this.sortAndLimit(pairs, limit);
	}

	private Collection<TransferBlockPair> getMultisigTransactionsForAccount(final Session session, final long accountId, final long maxId,
			final int limit, final ReadOnlyTransferDao.TransferType transferType, final int transactionType, final String joinField) {
		final List<TransactionIdBlockHeightPair> listOfIds = this.getMultisigIds(session, transferType, accountId, transactionType, maxId,
				limit);
		if (listOfIds.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigTransaction> transactions = this.getMultisigTransactions(session, listOfIds, joinField);
		final HashMap<Long, DbBlock> blockMap = this.getBlockMap(session, listOfIds);
		return IntStream.range(0, transactions.size())
				.mapToObj(i -> new TransferBlockPair(transactions.get(i), blockMap.get(listOfIds.get(i).blockHeight)))
				.collect(Collectors.toList());
	}

	private List<TransactionIdBlockHeightPair> getMultisigIds(final Session session, final ReadOnlyTransferDao.TransferType transferType,
			final long accountId, final int type, final long maxId, final int limit) {
		final String table = ReadOnlyTransferDao.TransferType.OUTGOING.equals(transferType) ? "multisigsends" : "multisigreceives";
		final String preQueryTemplate = "SELECT transactionId, height FROM " + table
				+ " WHERE accountId=%d AND type=%d AND transactionId < %d " // preserve-newline
				+ "ORDER BY accountId asc, type asc, transactionId DESC";
		final String preQueryString = String.format(preQueryTemplate, accountId, type, maxId);
		final Query preQuery = session.createSQLQuery(preQueryString) // preserve-newline
				.addScalar("transactionId", LongType.INSTANCE) // preserve-newline
				.addScalar("height", LongType.INSTANCE) // preserve-newline
				.setMaxResults(limit);
		final List<Object[]> list = HibernateUtils.listAndCast(preQuery);
		return list.stream().map(o -> new TransactionIdBlockHeightPair((Long) o[0], (Long) o[1])).collect(Collectors.toList());
	}

	private List<DbMultisigTransaction> getMultisigTransactions(final Session session, final List<TransactionIdBlockHeightPair> pairs,
			final String joinEntity) {
		final Criteria criteria = session.createCriteria(DbMultisigTransaction.class) // preserve-newline
				.setFetchMode(joinEntity, FetchMode.JOIN) // preserve-newline
				.add(Restrictions.in("id", pairs.stream().map(p -> p.transactionId).collect(Collectors.toList()))) // preserve-newline
				.add(Restrictions.isNotNull(joinEntity)) // preserve-newline
				.addOrder(Order.desc("id"));
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		final List<DbMultisigTransaction> result = HibernateUtils.listAndCast(criteria);
		// we deliberately set multisigSignatureTransactions to lazy, to avoid
		// duplicates inside other entities within current "joinEntity"
		// (i.e. TransferTransaction holds mosaics, so each mosaic would get duplicated
		// for every cosignatory)
		// now we need to force loading of child entities
		for (final DbMultisigTransaction transaction : result) {
			transaction.getMultisigSignatureTransactions().size();
		}
		return result;
	}

	private HashMap<Long, DbBlock> getBlockMap(final Session session, final List<TransactionIdBlockHeightPair> pairs) {
		final HashMap<Long, DbBlock> blockMap = new HashMap<>();
		final Criteria criteria = session.createCriteria(DbBlock.class) // preserve-newline
				.add(Restrictions.in("height", pairs.stream().map(p -> p.blockHeight).collect(Collectors.toList()))) // preserve-newline
				.addOrder(Order.desc("height"));
		final List<DbBlock> blocks = HibernateUtils.listAndCast(criteria);
		blocks.stream().forEach(b -> blockMap.put(b.getHeight(), b));
		return blockMap;
	}

	private Collection<TransferBlockPair> sortAndLimit(final Collection<TransferBlockPair> pairs, final int limit) {
		final List<TransferBlockPair> list = pairs.stream().sorted().collect(Collectors.toList());
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
