package org.nem.nis.service;

import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.dbmodel.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.MissingResourceException;

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
	public Transfer findByHash(byte[] txHash) {
		final Transfer transfer = transferDao.findByHash(txHash);
		if (null == transfer) {
			throw createMissingResourceException(txHash.toString());
		}
		return transfer;
	}

	@Override
	public Collection<Object[]> getTransactionsForAccount(final Account account, final Integer timestamp, int limit) {
		final Collection<Object[]> transfers = this.transferDao.getTransactionsForAccount(account, timestamp, limit);
		// TODO: throw execption
		return transfers;
	}

	private static MissingResourceException createMissingResourceException(final String key) {
		return new MissingResourceException("block not found in the db", Block.class.getName(), key);
	}
}
