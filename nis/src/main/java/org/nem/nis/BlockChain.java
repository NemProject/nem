package org.nem.nis;


import org.nem.core.dao.TransferDao;
import org.nem.core.dbmodel.Block;
import org.nem.core.model.Hashcode;
import org.nem.core.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class BlockChain {
	private ConcurrentMap<Hashcode, Transaction> unconfirmedTransactions;

	public BlockChain() {
		unconfirmedTransactions = new ConcurrentHashMap<>();
	}

	/**
	 *
	 * @param transaction - transaction that isValid() and verify()-ed
	 * @return
	 */
	public boolean processTransaction(Transaction transaction) {

		int currentTime = NisMain.getEpochTime();
		// rest is checked by isValid()
		if (transaction.getTimestamp() > currentTime + 30) {
			return false;
		}

		Hashcode transactionHash = new Hashcode(transaction.getHash());

		synchronized (BlockChain.class) {
			// TODO: check if transaction isn't already in DB
		}

		Transaction swapTest = unconfirmedTransactions.putIfAbsent(transactionHash, transaction);
		if (swapTest != null) {
			return false;
		}

		return true;
	}
}
