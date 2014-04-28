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

	/**
	 * Retrieves Transfer from db given it's hash.
	 *
	 * @param txHash hash of a transfer to retrieve.
	 * @return Transfer having given hash or null.
	 * @throws MissingResourceException If a matching transfer cannot be found.
	 */
	@Override
	public Transfer findByHash(byte[] txHash) {
		final Transfer transfer = transferDao.findByHash(txHash);
		if (null == transfer) {
			throw createMissingResourceException(txHash.toString());
		}
		return transfer;
	}

	/**
	 * Retrieves latest limit Transfers from db for given account
	 *
	 * @param account The account.
	 * @param limit The limit.
	 * @return (sorted?) Collection of Transfers
	 * @throws ?
	 */
	@Override
	public Collection<Transfer> getTransactionsForAccount(Account account, int limit) {
		final Collection<Transfer> transfers = this.transferDao.getTransactionsForAccount(account, limit);
		// TODO: throw execption
		return transfers;
	}

	private static MissingResourceException createMissingResourceException(final String key) {
		return new MissingResourceException("block not found in the db", Block.class.getName(), key);
	}
}
