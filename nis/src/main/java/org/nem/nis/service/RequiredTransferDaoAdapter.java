package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.dbmodel.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RequiredTransferDaoAdapter implements RequiredTransferDao {

	private final TransferDao transferDao;

	@Autowired(required = true)
	public RequiredTransferDaoAdapter(final TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	@Override
	public Long count() {
		return this.transferDao.count();
	}

	@Override
	public Transfer findByHash(final byte[] txHash) {
		final Transfer transfer = this.transferDao.findByHash(txHash);
		if (null == transfer) {
			throw createMissingResourceException(txHash.toString());
		}
		return transfer;
	}

	//TODO: we should probably delegate in getTransactionsForAccount too and add tests for delegation

	@Override
	public Collection<Object[]> getTransactionsForAccount(final Account account, final Integer timestamp, final int limit) {
		final Collection<Object[]> transfers = this.transferDao.getTransactionsForAccount(account, timestamp, limit);
		// TODO: throw exception
		return transfers;
	}

	@Override
	public Collection<Object[]> getTransactionsForAccountUsingHash(final Account account, final Hash hash, final TransferType transferType, final int limit) {
		// this can throw
		final Collection<Object[]> transfers = this.transferDao.getTransactionsForAccountUsingHash(account, hash, transferType, limit);
		return transfers;
	}

	private static MissingResourceException createMissingResourceException(final String key) {
		return new MissingResourceException("block not found in the db", Block.class.getName(), key);
	}
}
