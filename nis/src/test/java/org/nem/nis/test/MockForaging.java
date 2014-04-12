package org.nem.nis.test;

import org.nem.nis.Foraging;

public class MockForaging extends Foraging {
	public MockForaging() {
		super();
		this.setTransferDao(new MockTransferDaoImpl());
		this.setBlockChain(new MockBlockChain());
	}
}
