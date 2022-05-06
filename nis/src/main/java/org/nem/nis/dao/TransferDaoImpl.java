package org.nem.nis.dao;

import org.hibernate.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.TransactionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class TransferDaoImpl implements TransferDao {
	private final SessionFactory sessionFactory;

	/**
	 * Creates a transfer dao implementation.
	 *
	 * @param sessionFactory The session factory.
	 */
	@Autowired(required = true)
	public TransferDaoImpl(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	@Override
	@Transactional(readOnly = true)
	public TransferBlockPair getTransactionUsingHash(final Hash hash, final BlockHeight height) {
		return this.getTransferBlockPairUsingHash(hash, height);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<TransferBlockPair> getTransactionsForAccountUsingHash(final Account address, final Hash hash,
			final BlockHeight height, final TransferType transferType, final int limit) {
		final Long accountId = this.getAccountId(address);
		if (null == accountId) {
			return new ArrayList<>();
		}

		final long maxId = null == hash ? Long.MAX_VALUE : this.getTransferBlockPairUsingHash(hash, height).getTransfer().getId();
		return this.getTransactionsForAccountUsingId(address, maxId, transferType, limit);
	}

	@SuppressWarnings("rawtypes")
	private TransferBlockPair getTransferBlockPairUsingHash(final Hash hash, final BlockHeight height) {
		// since we know the block height and have to search for the hash in all transaction tables, the easiest way to do it
		// is simply to load the complete block from the db. It will be fast enough.
		final BlockLoader blockLoader = new BlockLoader(this.sessionFactory.getCurrentSession());
		final List<DbBlock> dbBlocks = blockLoader.loadBlocks(height, height);
		if (dbBlocks.isEmpty()) {
			throw new MissingResourceException("transaction not found in the db", Hash.class.toString(), hash.toString());
		}

		final DbBlock dbBlock = dbBlocks.get(0);
		for (final TransactionRegistry.Entry<AbstractBlockTransfer, ?> entry : TransactionRegistry.iterate()) {
			final List<AbstractBlockTransfer> transfers = entry.getFromBlock.apply(dbBlock).stream()
					.filter(t -> t.getTransferHash().equals(hash)).collect(Collectors.toList());
			if (!transfers.isEmpty()) {
				return new TransferBlockPair(transfers.get(0), dbBlock);
			}
		}

		throw new MissingResourceException("transaction not found in the db", Hash.class.toString(), hash.toString());
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<TransferBlockPair> getTransactionsForAccountUsingId(final Account address, final Long id,
			final TransferType transferType, final int limit) {
		final Long accountId = this.getAccountId(address);
		if (null == accountId) {
			return new ArrayList<>();
		}

		final long maxId = null == id ? Long.MAX_VALUE : id;
		return this.getTransactionsForAccountUpToTransaction(accountId, maxId, limit, transferType);
	}

	private Long getAccountId(final Account account) {
		return DaoUtils.getAccountId(this.getCurrentSession(), account.getAddress());
	}

	private Collection<TransferBlockPair> getTransactionsForAccountUpToTransaction(final Long accountId, final long maxId, final int limit,
			final TransferType transferType) {
		if (TransferType.ALL == transferType) {
			// note that we have to do separate queries for incoming and outgoing transactions since otherwise h2
			// is not able to use an index to speed up the query.
			final Collection<TransferBlockPair> pairs = this.getTransactionsForAccountUpToTransactionWithTransferType(accountId, maxId,
					limit, TransferType.INCOMING);
			pairs.addAll(this.getTransactionsForAccountUpToTransactionWithTransferType(accountId, maxId, limit, TransferType.OUTGOING));
			return this.sortAndLimit(pairs, limit);
		} else {
			final Collection<TransferBlockPair> pairs = this.getTransactionsForAccountUpToTransactionWithTransferType(accountId, maxId,
					limit, transferType);
			return this.sortAndLimit(pairs, limit);
		}
	}

	private Collection<TransferBlockPair> getTransactionsForAccountUpToTransactionWithTransferType(final Long accountId, final long maxId,
			final int limit, final TransferType transferType) {
		final Collection<TransferBlockPair> pairs = new ArrayList<>();
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			pairs.addAll(entry.getTransactionRetriever.get().getTransfersForAccount(this.getCurrentSession(), accountId, maxId, limit,
					transferType));
		}

		return pairs;
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
}
