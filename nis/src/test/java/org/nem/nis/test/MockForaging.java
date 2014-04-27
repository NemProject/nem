package org.nem.nis.test;

import org.nem.nis.BlockChain;
import org.nem.nis.Foraging;
import org.nem.nis.dao.TransferDao;

public class MockForaging extends Foraging {
	public MockForaging(TransferDao transferDao, BlockChain blockChain) {
		super();
		this.setTransferDao(transferDao);
		this.setBlockChain(blockChain);
	}

	public MockForaging() {
		this(new MockTransferDaoImpl(), new MockBlockChain());
	}
}
