package org.nem.nis.test;

import org.nem.nis.BlockChain;
import org.nem.nis.Foraging;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.peer.PeerNetwork;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockForaging extends Foraging {
	public MockForaging(TransferDao transferDao, BlockChain blockChain) {
		super();
		this.setTransferDao(transferDao);

		BlockChainLastBlockLayer lastBlockLayer = mock(BlockChainLastBlockLayer.class);
		this.setBlockChainLastBlockLayer(lastBlockLayer);
	}

	public MockForaging() {
		this(new MockTransferDaoImpl(), new MockBlockChain());
	}
}
