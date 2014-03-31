package org.nem.core.test;

import org.nem.nis.Foraging;

public class MockForaging extends Foraging {
	public MockForaging() {
		super();
		this.setTransferDao(new MockTransferDaoImpl());
	}
}
