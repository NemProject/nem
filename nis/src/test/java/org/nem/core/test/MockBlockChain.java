package org.nem.core.test;

import org.nem.nis.BlockChain;

public class MockBlockChain extends BlockChain {
	public MockBlockChain() {
		super();
		this.setTransferDao(new MockTransferDaoImpl());
	}
}
