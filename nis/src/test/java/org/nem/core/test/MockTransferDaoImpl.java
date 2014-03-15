package org.nem.core.test;

import org.nem.core.dao.TransferDao;
import org.nem.core.dbmodel.Transfer;

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
}
