package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.type.LongType;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class TransferDaoImpl implements TransferDao {
	private final SimpleTransferDaoImpl<Transfer> impl;

	@Autowired(required = true)
	public TransferDaoImpl(final SessionFactory sessionFactory) {
		this.impl = new SimpleTransferDaoImpl<>("Transfer", sessionFactory, Transfer::getTransferHash);
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
	public Transfer findByHash(final byte[] txHash) {
		return this.impl.findByHash(txHash);
	}

	@Override
	@Transactional(readOnly = true)
	public Transfer findByHash(final byte[] txHash, final long maxBlockHeight) {
		return this.impl.findByHash(txHash, maxBlockHeight);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean anyHashExists(final Collection<Hash> hashes, final BlockHeight maxBlockHeight) {
		return this.impl.anyHashExists(hashes, maxBlockHeight);
	}

	@Override
	@Transactional
	public void save(final Transfer entity) {
		this.impl.save(entity);
	}

	@Override
	@Transactional
	public void saveMulti(final List<Transfer> transfers) {
		this.impl.saveMulti(transfers);
	}

	//endregion

	// NOTE: this query will also ask for accounts of senders and recipients!
	@Override
	@Transactional(readOnly = true)
	public Collection<TransferBlockPair> getTransactionsForAccount(final Account address, final Integer timeStamp, final int limit) {
		// TODO: have no idea how to do it using Criteria...
		final Query query = this.getCurrentSession()
				.createQuery("select t, t.block from Transfer t " +
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
	@Transactional
	public Collection<TransferBlockPair> getTransactionsForAccountUsingHash(
			final Account address,
			final Hash hash,
			final BlockHeight height,
			final TransferType transferType,
			final int limit) {
		final Long accountId = this.getAccountId(address);
		if (hash == null) {
			return this.getLatestTransactionsForAccount(accountId, limit, transferType);
		} else {
			final String addressString = this.buildAddressQuery(transferType);
			final TransferBlockPair pair = this.getTransactionDescriptorUsingHash(accountId, hash, height, addressString);
			return this.getTransactionsForAccountUpToTransaction(accountId, limit, transferType, pair);
		}
	}

	private TransferBlockPair getTransactionDescriptorUsingHash(
			final Long accountId,
			final Hash hash,
			final BlockHeight height,
			final String addressString) {
		final Query prequery = this.getCurrentSession()
				.createQuery("select t, t.block from Transfer t " +
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

		for (TransferBlockPair pair : tempList) {
			if (pair.getTransfer().getTransferHash().equals(hash)) {
				return pair;
			}
		}

		throw new MissingResourceException("transaction not found in the db", Hash.class.toString(), hash.toString());
	}

	@Override
	@Transactional
	public Collection<TransferBlockPair> getTransactionsForAccountUsingId(
			final Account address,
			final Long id,
			final TransferType transferType,
			final int limit) {
		final Long accountId = this.getAccountId(address);
		if (id == null) {
			return this.getLatestTransactionsForAccount(accountId, limit, transferType);
		} else {
			final TransferBlockPair pair = this.getTransactionDescriptorUsingId(id);
			return this.getTransactionsForAccountUpToTransaction(accountId, limit, transferType, pair);
		}
	}

	private TransferBlockPair getTransactionDescriptorUsingId(final Long id) {
		final Query preQuery = this.getCurrentSession()
				.createQuery("select t, t.block from Transfer t WHERE t.id=:id")
				.setParameter("id", id);
		final List<TransferBlockPair> tempList = executeQuery(preQuery);
		if (tempList.size() < 1) {
			throw new MissingResourceException("transaction not found in the db", Hash.class.toString(), id.toString());
		}

		return tempList.get(0);
	}

	private Long getAccountId(final Account address) {
		final Query query = this.getCurrentSession()
				.createSQLQuery("select id as accountId from accounts WHERE printablekey=:address")
				.addScalar("accountId", LongType.INSTANCE)
				.setParameter("address", address.getAddress().getEncoded());
		return (Long)query.uniqueResult();
	}

	private Collection<TransferBlockPair> getTransactionsForAccountUpToTransaction(
			final Long accountId,
			final int limit,
			final TransferType transferType,
			final TransferBlockPair pair) {
		if (TransferType.ALL == transferType) {
			final Collection<TransferBlockPair> pairs =
					this.getTransactionsForAccountUpToTransactionWithTransferType(accountId, limit, TransferType.INCOMING, pair);
			pairs.addAll(this.getTransactionsForAccountUpToTransactionWithTransferType(accountId, limit, TransferType.OUTGOING, pair));
			return this.sortAndLimit(pairs, limit);
		} else {
			return this.getTransactionsForAccountUpToTransactionWithTransferType(accountId, limit, transferType, pair);
		}
	}

	private Collection<TransferBlockPair> getTransactionsForAccountUpToTransactionWithTransferType(
			final Long accountId,
			final int limit,
			final TransferType transferType,
			final TransferBlockPair pair) {
		final Query query;
		final Transfer topMostTransfer = pair.getTransfer();

		final String senderOrRecipient = TransferType.OUTGOING.equals(transferType)? "t.senderId" : "t.recipientId";
		final String preQueryString = "SELECT t.*, b.* " +
				"FROM transfers t LEFT OUTER JOIN Blocks b ON t.blockId = b.id " +
				"WHERE %s = %d AND t.id < %d AND t.blockId = b.id " +
				"ORDER BY %s, t.timestamp DESC, t.id DESC";
		final String queryString = String.format(preQueryString,
				senderOrRecipient,
				accountId,
				topMostTransfer.getId(),
				senderOrRecipient);
		query = this.getCurrentSession()
				.createSQLQuery(queryString)
				.addEntity(Transfer.class)
				.addEntity(Block.class)
				.setMaxResults(limit);
		return executeQuery(query);
	}

	private Collection<TransferBlockPair> getLatestTransactionsForAccount(
			final Long accountId,
			final int limit,
			final TransferType transferType) {
		// TODO 20141107 J-B: was something wrong with what buildAddressQuery was doing when transfer type was ALL?
		// TODO 20141108 BR -> J: I noticed that the /transaction/all request that NCC makes for th GUI was terrible slow (needed up to 3 seconds).
		// TODO                   The reason was the SQL query that hibernate created which was far from being optimal. I would have preferred a SQL query
		// TODO                   using UNION keyword. But hibernate doesn't support unions even if I explicitly give hibernate a query string using UNION.
		// TODO                   So I ended up doing two queries and building the union/doing sorting "manually". It is still more than ten times
		// TODO                   faster then the original query hibernate creates.
		if (TransferType.ALL == transferType) {
			final Collection<TransferBlockPair> pairs = this.getLatestTransactionsForAccountWithTransferType(accountId, limit, TransferType.INCOMING);
			pairs.addAll(this.getLatestTransactionsForAccountWithTransferType(accountId, limit, TransferType.OUTGOING));
			return this.sortAndLimit(pairs, limit);
		} else {
			return this.getLatestTransactionsForAccountWithTransferType(accountId, limit, transferType);
		}
	}

	private Collection<TransferBlockPair> getLatestTransactionsForAccountWithTransferType(
			final Long accountId,
			final int limit,
			final TransferType transferType) {
		final Query query;
		final String senderOrRecipient = TransferType.OUTGOING.equals(transferType)? "t.senderId" : "t.recipientId";
		final String preQueryString = "SELECT t.*, b.* " +
				"FROM transfers t LEFT OUTER JOIN Blocks b ON t.blockId = b.id " +
				"WHERE %s = %d AND t.blockId = b.id " +
				"ORDER BY %s, t.timestamp DESC, t.id DESC";
		final String queryString = String.format(preQueryString,
				senderOrRecipient,
				accountId,
				senderOrRecipient);
		query = this.getCurrentSession()
				.createSQLQuery(queryString)
				.addEntity(Transfer.class)
				.addEntity(Block.class)
				.setMaxResults(limit);
		return executeQuery(query);
	}

	@SuppressWarnings("unchecked")
	private static List<TransferBlockPair> executeQuery(final Query q) {
		// TODO BR: I am too stupid to do it with streams :/
		final List<TransferBlockPair> pairs = new ArrayList<>();
		final List<Object[]> list = q.list();
		for (Object[] o : list) {
			pairs.add(new TransferBlockPair((Transfer)o[0], (Block)o[1]));
		}

		return pairs;
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
		final Transfer lhsTransfer = lhs.getTransfer();
		final Long lhsHeight = lhs.getBlock().getHeight();
		final Transfer rhsTransfer = rhs.getTransfer();
		final Long rhsHeight = rhs.getBlock().getHeight();

		final int heightComparison = -lhsHeight.compareTo(rhsHeight);
		if (0 != heightComparison) {
			return heightComparison;
		}

		final int timeStampComparison = -lhsTransfer.getTimeStamp().compareTo(rhsTransfer.getTimeStamp());
		if (0 != timeStampComparison) {
			return timeStampComparison;
		}

		final int blockIndexComparison = lhsTransfer.getBlkIndex().compareTo(rhsTransfer.getBlkIndex());
		if (0 != blockIndexComparison) {
			return blockIndexComparison;
		}

		return 0;
	}
}
