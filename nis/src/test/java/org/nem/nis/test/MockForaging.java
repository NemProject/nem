package org.nem.nis.test;

import org.nem.core.serialization.AccountLookup;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.nem.nis.Foraging;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.peer.PeerNetwork;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockForaging extends Foraging {
	// final AccountLookup accountLookup, final BlockDao blockDao, final BlockChainLastBlockLayer blockChainLastBlockLayer, final TransferDao transferDao) {
	public MockForaging(final AccountLookup accountLookup, final BlockDao blockDao, final BlockChainLastBlockLayer blockChainLastBlockLayer, final TransferDao transferDao) {
		super(accountLookup, blockDao, blockChainLastBlockLayer, transferDao);
	}

	public MockForaging(AccountAnalyzer accountAnalyzer, BlockChainLastBlockLayer lastBlockLayer) {
		this(accountAnalyzer, new MockBlockDao(null), lastBlockLayer, new MockTransferDaoImpl());
	}
}
