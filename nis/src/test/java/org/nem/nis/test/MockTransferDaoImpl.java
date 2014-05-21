package org.nem.nis.test;

import org.nem.core.model.Account;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.dbmodel.Transfer;

import java.util.Collection;
import java.util.List;

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
	public Collection<Object[]> getTransactionsForAccount(Account account, int limit) {
		return null;
	}
}
