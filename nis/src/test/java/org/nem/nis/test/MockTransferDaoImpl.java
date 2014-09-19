package org.nem.nis.test;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.dbmodel.Transfer;

import java.util.*;

public class MockTransferDaoImpl implements TransferDao {
	@Override
	public void save(final Transfer transfer) {
		throw new RuntimeException("not needed");
	}

	@Override
	public Long count() {
		throw new RuntimeException("not needed");
	}

	@Override
	public void saveMulti(final List<Transfer> transfers) {
		throw new RuntimeException("not needed");
	}

	@Override
	public Transfer findByHash(final byte[] txHash) {
		return null;
	}

	@Override
	public Transfer findByHash(final byte[] txHash, long maxBlockHeight) {
		return null;
	}

	@Override
	public Collection<Object[]> getTransactionsForAccount(final Account account, final Integer timeStamp, final int limit) {
		return null;
	}

	@Override
	public Collection<Object[]> getTransactionsForAccountUsingHash(final Account account, final Hash hash, final TransferType transferType, final int limit) {
		return null;
	}
}
