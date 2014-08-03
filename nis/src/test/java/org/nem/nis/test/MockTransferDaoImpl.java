package org.nem.nis.test;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.dbmodel.Transfer;

import java.util.*;

public class MockTransferDaoImpl implements TransferDao {
	@Override
	public void save(Transfer transfer) {
		throw new RuntimeException("not needed");
	}

	@Override
	public Long count() {
		throw new RuntimeException("not needed");
	}

	@Override
	public void saveMulti(List<Transfer> transfers) {
		throw new RuntimeException("not needed");
	}

	@Override
	public Transfer findByHash(byte[] txHash) {
		return null;
	}

	@Override
	public Collection<Object[]> getTransactionsForAccount(final Account account, final Integer timestamp, int limit) {
		return null;
	}

	@Override
	public Collection<Object[]> getTransactionsForAccountUsingHash(final Account account, final Hash hash, final TransferType transferType, int limit) {
		return null;
	}
}
